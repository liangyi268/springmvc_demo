package com.sky.service;

import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.result.PageResult;
import com.sky.vo.DishVO;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface DishService {
    @Transactional
    void saveWithFlavor(DishDTO dishDTO);

    PageResult page(DishPageQueryDTO dishPageQueryDTO );

    void delete(List<Long> ids);

    DishVO getByIdWithFlavor(Long id);

    void update(DishDTO dishDTO);

    void startOrStop(Integer status, Long id);

    List<DishVO> list(Dish dish);

    List<DishVO> listWithFavour(Dish dish);
}
