package ru.practicum.main.category.service;

import ru.practicum.main.category.dto.CategoryDto;
import ru.practicum.main.category.dto.NewCategoryDto;
import java.util.List;

public interface CategoryService {
    CategoryDto create(NewCategoryDto dto);

    void delete(Long catId);

    CategoryDto update(Long catId, CategoryDto dto);

    List<CategoryDto> getAll(Integer from, Integer size);

    CategoryDto getById(Long catId);

}
