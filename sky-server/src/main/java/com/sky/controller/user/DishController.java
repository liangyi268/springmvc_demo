package com.sky.controller.user;

import com.sky.constant.StatusConstant;
import com.sky.entity.Dish;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.DishService;
import com.sky.vo.DishVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController("user-dish")
@RequestMapping("/user/dish")
@Api(tags = "C端菜品接口")
@Slf4j
public class DishController {

    @Autowired
    private DishService dishService;
    @GetMapping("/list")
    @ApiOperation("查询菜品")
    public Result<List<DishVO>> list(Long categoryId){
        log.info("查询菜品{}",categoryId);
        Dish dish = new Dish();
        dish.setCategoryId(categoryId);
        dish.setStatus(StatusConstant.ENABLE);
        return Result.success(dishService.listWithFavour(dish));
    }
}
