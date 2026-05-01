package ru.practicum.main.rating.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.main.rating.model.Rating;
import ru.practicum.main.rating.model.RatingKey;

public interface RatingRepository extends JpaRepository<Rating, RatingKey> {

    long countByEventIdAndIsLikeTrue(Long eventId);

    long countByEventIdAndIsLikeFalse(Long eventId);

    @Query("SELECT COALESCE(SUM(CASE WHEN r.isLike = true THEN 1 ELSE -1 END), 0) " +
            "FROM Rating r WHERE r.event.initiator.id = :userId")
    Long getAuthorRating(@Param("userId") Long userId);

    void deleteByEventIdAndUserId(Long eventId, Long userId);

}
