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
import org.springframework.data.redis.core.RedisTemplate;
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
    @Autowired
    private RedisTemplate redisTemplate;
    /**
     * 获取指定菜品的详细信息
     * @param categoryId
     * @return
     */
    @GetMapping("/list")
    @ApiOperation("查询菜品")
    public Result<List<DishVO>> list(Long categoryId){
        //构造redis的key，规则: dish_categoryId
        String key = "dish_" + categoryId;

        //查询redis中是否缓存了数据
        List<DishVO> list = (List<DishVO>) redisTemplate.opsForValue().get(key);

        if(list != null && list.size() > 0){
            //如果缓存有数据，则直接返回
            return Result.success(list);
        }

        Dish dish = new Dish();
        dish.setCategoryId(categoryId);
        dish.setStatus(StatusConstant.ENABLE);
        //缓存中没有数据，则查询数据库，并缓存数据
        list = dishService.listWithFavour(dish);
        redisTemplate.opsForValue().set(key,list);
        return Result.success(list);
    }
}
