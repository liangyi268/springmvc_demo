package com.sky.service;

import com.sky.dto.CategoryDTO;
import com.sky.dto.CategoryPageQueryDTO;
import com.sky.entity.Category;
import com.sky.result.PageResult;
import org.springframework.stereotype.Service;


public interface CategoryService {

    void save(CategoryDTO categoryDTO);

    PageResult page(CategoryPageQueryDTO categoryPageQueryDTO);

    void startOrStop(Integer status, Long id);

    void update(CategoryDTO categoryDTO);

    void delete(Long id);

    Category list(Integer type);

//    PageResult list(Integer );
}
