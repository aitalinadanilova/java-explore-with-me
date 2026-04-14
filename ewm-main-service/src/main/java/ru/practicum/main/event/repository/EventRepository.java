package ru.practicum.main.event.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.practicum.main.event.model.Event;
import org.springframework.data.domain.Pageable;

public interface EventRepository extends JpaRepository<Event, Long> {
    Page<Event> findAllByInitiatorId(Long initiatorId, Pageable pageable);

    boolean existsByCategoryId(Long categoryId);

}
