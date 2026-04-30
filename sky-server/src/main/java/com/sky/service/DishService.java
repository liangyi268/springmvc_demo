package com.sky.service;

import com.sky.dto.DishDTO;
import org.springframework.transaction.annotation.Transactional;

public interface DishService {
    @Transactional
    void saveWithFlavor(DishDTO dishDTO);
}
