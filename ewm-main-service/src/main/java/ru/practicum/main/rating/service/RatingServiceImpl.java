package ru.practicum.main.rating.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.main.event.model.Event;
import ru.practicum.main.event.model.EventState;
import ru.practicum.main.event.repository.EventRepository;
import ru.practicum.main.exception.ConflictException;
import ru.practicum.main.exception.NotFoundException;
import ru.practicum.main.rating.model.Rating;
import ru.practicum.main.rating.repository.RatingRepository;
import ru.practicum.main.request.model.RequestStatus;
import ru.practicum.main.request.repository.RequestRepository;
import ru.practicum.main.user.model.User;
import ru.practicum.main.user.repository.UserRepository;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RatingServiceImpl implements RatingService {
    private final RatingRepository ratingRepository;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final RequestRepository requestRepository;

    @Override
    @Transactional
    public void addLike(Long userId, Long eventId) {
        saveRating(userId, eventId, true);
    }

    @Override
    @Transactional
    public void addDislike(Long userId, Long eventId) {
        saveRating(userId, eventId, false);
    }

    @Override
    @Transactional
    public void removeRating(Long userId, Long eventId) {
        if (!userRepository.existsById(userId)) throw new NotFoundException("User not found");
        if (!eventRepository.existsById(eventId)) throw new NotFoundException("Event not found");
        ratingRepository.deleteByEventIdAndUserId(eventId, userId);
    }

    private void saveRating(Long userId, Long eventId, boolean isLike) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event not found"));

        if (event.getInitiator().getId().equals(userId)) {
            throw new ConflictException("Initiator cannot rate their own event");
        }

        if (event.getState() != EventState.PUBLISHED) {
            throw new ConflictException("Cannot rate unpublished event");
        }

        boolean isParticipant = requestRepository.findAllByEventId(eventId).stream()
                .anyMatch(r -> r.getRequester().getId().equals(userId) && r.getStatus() == RequestStatus.CONFIRMED);

        if (!isParticipant) {
            throw new ConflictException("Only confirmed participants can rate the event");
        }

        Rating rating = Rating.builder()
                .user(user)
                .event(event)
                .isLike(isLike)
                .build();

        ratingRepository.save(rating);
    }

}
