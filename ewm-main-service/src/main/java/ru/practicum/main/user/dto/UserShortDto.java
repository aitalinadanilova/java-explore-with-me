package ru.practicum.main.user.dto;

import lombok.*;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserShortDto {
    private Long id;

    private String name;

    private Long rating;

}
