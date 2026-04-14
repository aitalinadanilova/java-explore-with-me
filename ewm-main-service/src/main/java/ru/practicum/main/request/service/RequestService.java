package ru.practicum.main.request.service;

import ru.practicum.main.request.dto.ParticipationRequestDto;
import java.util.List;

public interface RequestService {
    ParticipationRequestDto createRequest(Long userId, Long eventId);

    List<ParticipationRequestDto> getUserRequests(Long userId);

    ParticipationRequestDto cancelRequest(Long userId, Long requestId);

}
