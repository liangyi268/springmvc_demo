package com.sky.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.constant.StatusConstant;
import com.sky.context.BaseContext;
import com.sky.dto.CategoryDTO;
import com.sky.dto.CategoryPageQueryDTO;
import com.sky.entity.Category;
import com.sky.exception.BaseException;
import com.sky.mapper.CategoryMapper;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.result.PageResult;
import com.sky.service.CategoryService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class CategoryServiceImpl implements CategoryService {

    @Autowired
    private CategoryMapper categoryMapper;
    @Autowired
    private DishMapper dishMapper;
    @Autowired
    private SetmealMapper setmealMapper;
    @Override
    public void save(CategoryDTO categoryDTO) {
        Category category =new Category();
        BeanUtils.copyProperties(categoryDTO, category);
        //设置分类状态
        category.setStatus(StatusConstant.DISABLE);
        //设置创建时间，分类时间
//        category.setCreateTime(LocalDateTime.now());
//        category.setUpdateTime(LocalDateTime.now());
//        //设置创建人，修改人
//        category.setCreateUser(BaseContext.getCurrentId());
//        category.setUpdateUser(BaseContext.getCurrentId());
        //插入数据
        categoryMapper.save(category);
    }

    @Override
    public PageResult page(CategoryPageQueryDTO categoryPageQueryDTO) {
        PageHelper.startPage(categoryPageQueryDTO.getPage(),categoryPageQueryDTO.getPageSize());
        Page<Category> categoryPage = categoryMapper.page(categoryPageQueryDTO);
        PageResult pageResult =new PageResult();
        pageResult.setTotal(categoryPage.getTotal());
        pageResult.setRecords(categoryPage.getResult());
        return pageResult;
    }

    @Override
    public void startOrStop(Integer status, Long id) {
        Category category =new Category();
        category.setId(id);
        category.setStatus(status);
//        category.setUpdateTime(LocalDateTime.now());
//        category.setUpdateUser(BaseContext.getCurrentId());
        categoryMapper.update(category);
    }

    @Override
    public void update(CategoryDTO categoryDTO) {
        Category category =new Category();
        BeanUtils.copyProperties(categoryDTO, category);
//        category.setUpdateTime(LocalDateTime.now());
//        category.setUpdateUser(BaseContext.getCurrentId());
        categoryMapper.update(category);
    }

    @Override
    public void delete(Long id) {
        //判断当前分类是否关联了菜品，如果已经关联，则无法删除
        Integer count = dishMapper.countByCategoryId(id);
        if(count > 0){
            //当前分类关联了菜品，无法删除
            throw new BaseException(MessageConstant.CATEGORY_BE_RELATED_BY_DISH);
        }
        //判断当前分类是否关联了套餐，如果已经关联，则无法删除
        count = setmealMapper.countByCategoryId(id);
        if(count > 0){
            //当前分类关联了套餐，无法删除
            throw new BaseException(MessageConstant.CATEGORY_BE_RELATED_BY_SETMEAL);
        }
        categoryMapper.delete(id);
    }

    @Override
    public List<Category> list(Integer type) {
        return categoryMapper.list(type);
    }

//    @Override
//    public PageResult list(Integer type) {
//        return categoryMapper.list(type);
//    }
}
