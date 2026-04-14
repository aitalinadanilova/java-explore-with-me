package ru.practicum.main.event.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.Predicate;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.dto.ViewStatsDto;
import ru.practicum.main.category.model.Category;
import ru.practicum.main.category.repository.CategoryRepository;
import ru.practicum.main.event.dto.*;
import ru.practicum.main.event.mapper.EventMapper;
import ru.practicum.main.event.model.Event;
import ru.practicum.main.event.model.EventState;
import ru.practicum.main.event.repository.EventRepository;
import ru.practicum.main.exception.BadRequestException;
import ru.practicum.main.exception.ConflictException;
import ru.practicum.main.exception.NotFoundException;
import ru.practicum.main.request.dto.EventRequestStatusUpdateRequest;
import ru.practicum.main.request.dto.EventRequestStatusUpdateResult;
import ru.practicum.main.request.dto.ParticipationRequestDto;
import ru.practicum.main.request.mapper.RequestMapper;
import ru.practicum.main.request.model.ParticipationRequest;
import ru.practicum.main.request.model.RequestStatus;
import ru.practicum.main.request.repository.RequestRepository;
import ru.practicum.main.user.model.User;
import ru.practicum.main.user.repository.UserRepository;
import ru.practicum.client.StatsClient;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EventServiceImpl implements EventService {
    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final RequestRepository requestRepository;
    private final EntityManager entityManager;
    private final StatsClient statsClient;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public EventFullDto createEvent(Long userId, NewEventDto newEventDto) {
        if (newEventDto.getEventDate().isBefore(LocalDateTime.now().plusHours(2))) {
            throw new ConflictException("Event date must be in the future");
        }
        User initiator = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));
        Category category = categoryRepository.findById(newEventDto.getCategory())
                .orElseThrow(() -> new NotFoundException("Category not found"));

        Event event = EventMapper.toEvent(newEventDto, category, initiator);
        event.setState(EventState.PENDING);
        event.setCreatedOn(LocalDateTime.now());
        event.setConfirmedRequests(0L);

        EventFullDto dto = EventMapper.toEventFullDto(eventRepository.save(event));
        dto.setViews(0L);
        return dto;
    }

    @Override
    public List<EventShortDto> getEventsByUserId(Long userId, Integer from, Integer size) {
        if (!userRepository.existsById(userId)) throw new NotFoundException("User not found");
        List<Event> events = eventRepository.findAllByInitiatorId(userId, PageRequest.of(from / size, size))
                .getContent();
        return mapToShortDtoWithViews(events);
    }

    @Override
    public EventFullDto getEventById(Long userId, Long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event not found"));
        if (!event.getInitiator().getId().equals(userId)) {
            throw new ConflictException("User is not the initiator");
        }

        EventFullDto dto = EventMapper.toEventFullDto(event);
        dto.setViews(getViewsForEvent(event));
        return dto;
    }

    @Override
    @Transactional
    public EventFullDto updateEventUser(Long userId, Long eventId, UpdateEventUserRequest request) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event not found"));

        if (event.getState() == EventState.PUBLISHED) {
            throw new ConflictException("Only pending or canceled events can be changed");
        }

        updateEventFields(event, request);

        if (request.getStateAction() != null) {
            if (request.getStateAction().equals("SEND_TO_REVIEW")) event.setState(EventState.PENDING);
            else if (request.getStateAction().equals("CANCEL_REVIEW")) event.setState(EventState.CANCELED);
        }

        EventFullDto dto = EventMapper.toEventFullDto(eventRepository.save(event));
        dto.setViews(getViewsForEvent(event));
        return dto;
    }

    @Override
    public List<EventFullDto> getEventsAdmin(List<Long> users, List<String> states, List<Long> categories,
                                             LocalDateTime rangeStart, LocalDateTime rangeEnd, Integer from, Integer size) {
        var cb = entityManager.getCriteriaBuilder();
        var query = cb.createQuery(Event.class);
        var root = query.from(Event.class);
        var predicates = new ArrayList<Predicate>();

        if (users != null && !users.isEmpty()) predicates.add(root.get("initiator").get("id").in(users));
        if (states != null && !states.isEmpty()) {
            List<EventState> evStates = states.stream().map(EventState::valueOf).collect(Collectors.toList());
            predicates.add(root.get("state").in(evStates));
        }
        if (categories != null && !categories.isEmpty()) predicates.add(root.get("category").get("id").in(categories));
        if (rangeStart != null) predicates.add(cb.greaterThanOrEqualTo(root.get("eventDate"), rangeStart));
        if (rangeEnd != null) predicates.add(cb.lessThanOrEqualTo(root.get("eventDate"), rangeEnd));

        query.where(predicates.toArray(new Predicate[0]));
        List<Event> events = entityManager.createQuery(query).setFirstResult(from).setMaxResults(size).getResultList();

        return mapToFullDtoWithViews(events);
    }

    @Override
    @Transactional
    public EventFullDto updateEventAdmin(Long eventId, UpdateEventAdminRequest request) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event with id=" + eventId + " was not found"));

        if (request.getEventDate() != null && request.getEventDate().isBefore(LocalDateTime.now().plusHours(1))) {
            throw new ConflictException("Event date must be at least 1 hour before publication");
        }

        if (request.getStateAction() != null) {
            if (request.getStateAction().equals("PUBLISH_EVENT")) {
                if (event.getState() != EventState.PENDING) {
                    throw new ConflictException("Cannot publish the event because it's not in PENDING state");
                }
                event.setState(EventState.PUBLISHED);
                event.setPublishedOn(LocalDateTime.now());
            } else if (request.getStateAction().equals("REJECT_EVENT")) {
                if (event.getState() == EventState.PUBLISHED) {
                    throw new ConflictException("Cannot reject the event because it's already PUBLISHED");
                }
                event.setState(EventState.CANCELED);
            }
        }

        updateEventFieldsAdmin(event, request);

        EventFullDto dto = EventMapper.toEventFullDto(eventRepository.save(event));
        dto.setViews(getViewsForEvent(event));
        return dto;
    }

    @Override
    public List<ParticipationRequestDto> getEventRequests(Long userId, Long eventId) {
        return requestRepository.findAllByEventId(eventId).stream()
                .map(RequestMapper::toDto).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public EventRequestStatusUpdateResult updateRequestStatus(Long userId, Long eventId, EventRequestStatusUpdateRequest updateRequest) {
        Event event = eventRepository.findById(eventId).orElseThrow(() -> new NotFoundException("Event not found"));
        Long confirmedCount = requestRepository.countByEventIdAndStatus(eventId, RequestStatus.CONFIRMED);

        List<ParticipationRequest> requests = requestRepository.findAllByIdIn(updateRequest.getRequestIds());
        EventRequestStatusUpdateResult result = new EventRequestStatusUpdateResult(new ArrayList<>(), new ArrayList<>());

        for (ParticipationRequest req : requests) {
            if (req.getStatus() != RequestStatus.PENDING) {
                throw new ConflictException("Request must have status PENDING");
            }

            if (updateRequest.getStatus().equals("CONFIRMED")) {
                if (event.getParticipantLimit() == 0 || confirmedCount < event.getParticipantLimit()) {
                    req.setStatus(RequestStatus.CONFIRMED);
                    confirmedCount++;
                    result.getConfirmedRequests().add(RequestMapper.toDto(req));
                } else {
                    req.setStatus(RequestStatus.REJECTED);
                    result.getRejectedRequests().add(RequestMapper.toDto(req));
                }
            } else {
                req.setStatus(RequestStatus.REJECTED);
                result.getRejectedRequests().add(RequestMapper.toDto(req));
            }
        }
        requestRepository.saveAll(requests);
        return result;
    }

    @Override
    public List<EventShortDto> getEventsPublic(String text, List<Long> categories, Boolean paid,
                                               LocalDateTime rangeStart, LocalDateTime rangeEnd,
                                               Boolean onlyAvailable, String sort,
                                               Integer from, Integer size, HttpServletRequest request) {

        if (rangeStart != null && rangeEnd != null && rangeStart.isAfter(rangeEnd)) {
            throw new BadRequestException("Start date must be before end date");
        }

        var cb = entityManager.getCriteriaBuilder();
        var query = cb.createQuery(Event.class);
        var root = query.from(Event.class);
        var predicates = new ArrayList<Predicate>();

        predicates.add(cb.equal(root.get("state"), EventState.PUBLISHED));

        if (text != null && !text.isBlank()) {
            String pattern = "%" + text.toLowerCase() + "%";
            predicates.add(cb.or(
                    cb.like(cb.lower(root.get("annotation")), pattern),
                    cb.like(cb.lower(root.get("description")), pattern)
            ));
        }

        if (categories != null && !categories.isEmpty()) predicates.add(root.get("category").get("id").in(categories));
        if (paid != null) predicates.add(cb.equal(root.get("paid"), paid));

        if (rangeStart == null && rangeEnd == null) {
            predicates.add(cb.greaterThan(root.get("eventDate"), LocalDateTime.now()));
        } else {
            if (rangeStart != null) predicates.add(cb.greaterThanOrEqualTo(root.get("eventDate"), rangeStart));
            if (rangeEnd != null) predicates.add(cb.lessThanOrEqualTo(root.get("eventDate"), rangeEnd));
        }

        if (onlyAvailable) {
            predicates.add(cb.or(
                    cb.equal(root.get("participantLimit"), 0),
                    cb.lessThan(root.get("confirmedRequests"), root.get("participantLimit"))
            ));
        }

        query.where(predicates.toArray(new Predicate[0]));
        if ("EVENT_DATE".equals(sort)) query.orderBy(cb.asc(root.get("eventDate")));

        List<Event> events = entityManager.createQuery(query).setFirstResult(from).setMaxResults(size).getResultList();
        statsClient.postHit("ewm-main-service", request.getRequestURI(), request.getRemoteAddr(), LocalDateTime.now());

        return mapToShortDtoWithViews(events);
    }

    @Override
    public EventFullDto getEventByIdPublic(Long id, HttpServletRequest request) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Event with id=" + id + " not found"));

        if (event.getState() != EventState.PUBLISHED) {
            throw new NotFoundException("Event must be published");
        }

        statsClient.postHit("ewm-main-service", request.getRequestURI(), request.getRemoteAddr(), LocalDateTime.now());

        EventFullDto dto = EventMapper.toEventFullDto(event);
        dto.setViews(getViewsForEvent(event));
        return dto;
    }

    private Long getViewsForEvent(Event event) {
        if (event.getPublishedOn() == null) return 0L;

        ResponseEntity<Object> response = statsClient.getStats(
                event.getPublishedOn(),
                LocalDateTime.now(),
                List.of("/events/" + event.getId()),
                true);

        List<ViewStatsDto> stats = objectMapper.convertValue(response.getBody(), new TypeReference<List<ViewStatsDto>>() {});

        return stats.isEmpty() ? 0L : stats.get(0).getHits();
    }

    private List<EventShortDto> mapToShortDtoWithViews(List<Event> events) {
        if (events.isEmpty()) return Collections.emptyList();
        Map<Long, Long> viewsMap = getViewsMap(events);
        return events.stream()
                .map(e -> {
                    EventShortDto dto = EventMapper.toEventShortDto(e);
                    dto.setViews(viewsMap.getOrDefault(e.getId(), 0L));
                    return dto;
                }).collect(Collectors.toList());
    }

    private List<EventFullDto> mapToFullDtoWithViews(List<Event> events) {
        if (events.isEmpty()) return Collections.emptyList();
        Map<Long, Long> viewsMap = getViewsMap(events);
        return events.stream()
                .map(e -> {
                    EventFullDto dto = EventMapper.toEventFullDto(e);
                    dto.setViews(viewsMap.getOrDefault(e.getId(), 0L));
                    return dto;
                }).collect(Collectors.toList());
    }

    private Map<Long, Long> getViewsMap(List<Event> events) {
        LocalDateTime start = events.stream()
                .map(Event::getPublishedOn)
                .filter(Objects::nonNull)
                .min(LocalDateTime::compareTo)
                .orElse(LocalDateTime.now().minusYears(1));

        List<String> uris = events.stream()
                .map(e -> "/events/" + e.getId())
                .collect(Collectors.toList());

        ResponseEntity<Object> response = statsClient.getStats(start, LocalDateTime.now(), uris, true);
        List<ViewStatsDto> stats = objectMapper.convertValue(response.getBody(), new TypeReference<List<ViewStatsDto>>() {});

        return stats.stream()
                .collect(Collectors.toMap(
                        s -> Long.parseLong(s.getUri().substring(8)),
                        ViewStatsDto::getHits
                ));
    }

    private void updateEventFields(Event event, UpdateEventUserRequest request) {
        if (request.getAnnotation() != null) event.setAnnotation(request.getAnnotation());
        if (request.getDescription() != null) event.setDescription(request.getDescription());
        if (request.getPaid() != null) event.setPaid(request.getPaid());
        if (request.getParticipantLimit() != null) event.setParticipantLimit(request.getParticipantLimit());
        if (request.getRequestModeration() != null) event.setRequestModeration(request.getRequestModeration());
        if (request.getTitle() != null) event.setTitle(request.getTitle());
        if (request.getCategory() != null) {
            event.setCategory(categoryRepository.findById(request.getCategory())
                    .orElseThrow(() -> new NotFoundException("Category not found")));
        }
        if (request.getLocation() != null) {
            event.setLocation(EventMapper.toLocation(request.getLocation()));
        }
    }

    private void updateEventFieldsAdmin(Event event, UpdateEventAdminRequest request) {
        if (request.getAnnotation() != null) event.setAnnotation(request.getAnnotation());
        if (request.getDescription() != null) event.setDescription(request.getDescription());
        if (request.getPaid() != null) event.setPaid(request.getPaid());
        if (request.getParticipantLimit() != null) event.setParticipantLimit(request.getParticipantLimit());
        if (request.getRequestModeration() != null) event.setRequestModeration(request.getRequestModeration());
        if (request.getTitle() != null) event.setTitle(request.getTitle());
        if (request.getCategory() != null) {
            event.setCategory(categoryRepository.findById(request.getCategory())
                    .orElseThrow(() -> new NotFoundException("Category not found")));
        }
        if (request.getLocation() != null) {
            event.setLocation(EventMapper.toLocation(request.getLocation()));
        }
    }
}
