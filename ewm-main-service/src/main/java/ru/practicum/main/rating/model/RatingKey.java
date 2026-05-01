package ru.practicum.main.rating.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RatingKey implements Serializable {
    private Long event;

    private Long user;

}
