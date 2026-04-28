package ru.practicum.main.request.mapper;


import lombok.experimental.UtilityClass;
import ru.practicum.main.request.dto.EventRequestStatusUpdateResult;
import ru.practicum.main.request.dto.ParticipationRequestDto;
import ru.practicum.main.request.model.ParticipationRequest;
import ru.practicum.main.request.model.RequestStatus;

import java.util.List;
import java.util.stream.Collectors;

@UtilityClass
public class RequestMapper {
    public static ParticipationRequestDto toDto(ParticipationRequest request) {
        return ParticipationRequestDto.builder()
                .id(request.getId())
                .created(request.getCreated())
                .event(request.getEvent().getId())
                .requester(request.getRequester().getId())
                .status(request.getStatus().toString())
                .build();
    }

    public static EventRequestStatusUpdateResult toStatusResult(List<ParticipationRequest> requests) {
        List<ParticipationRequestDto> confirmed = requests.stream()
                .filter(r -> r.getStatus() == RequestStatus.CONFIRMED)
                .map(RequestMapper::toDto)
                .collect(Collectors.toList());

        List<ParticipationRequestDto> rejected = requests.stream()
                .filter(r -> r.getStatus() == RequestStatus.REJECTED)
                .map(RequestMapper::toDto)
                .collect(Collectors.toList());

        return new EventRequestStatusUpdateResult(confirmed, rejected);
    }

}
