package ru.practicum.main.event.dto;

import lombok.*;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LocationDto {
    private Float lat;

    private Float lon;

}
