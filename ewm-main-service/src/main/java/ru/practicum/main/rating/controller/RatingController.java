package ru.practicum.main.rating.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.practicum.main.rating.service.RatingService;

@RestController
@RequestMapping("/users/{userId}/events/{eventId}/rating")
@RequiredArgsConstructor
public class RatingController {
    private final RatingService ratingService;

    @PostMapping("/like")
    @ResponseStatus(HttpStatus.CREATED)
    public void addLike(@PathVariable Long userId, @PathVariable Long eventId) {
        ratingService.addLike(userId, eventId);
    }

    @PostMapping("/dislike")
    @ResponseStatus(HttpStatus.CREATED)
    public void addDislike(@PathVariable Long userId, @PathVariable Long eventId) {
        ratingService.addDislike(userId, eventId);
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeRating(@PathVariable Long userId, @PathVariable Long eventId) {
        ratingService.removeRating(userId, eventId);
    }

}
