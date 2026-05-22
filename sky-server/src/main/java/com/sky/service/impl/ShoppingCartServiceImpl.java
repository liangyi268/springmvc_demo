package com.sky.service.impl;

import com.sky.context.BaseContext;
import com.sky.dto.ShoppingCartDTO;
import com.sky.entity.Dish;
import com.sky.entity.Setmeal;
import com.sky.entity.ShoppingCart;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.mapper.ShoppingCartMapper;
import com.sky.service.ShoppingCartService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
public class ShoppingCartServiceImpl implements ShoppingCartService {

    @Autowired
    private ShoppingCartMapper shoppingCartMapper;
    @Autowired
    private DishMapper dishMapper;
    @Autowired
    private SetmealMapper setmealMapper;

    @Override
    public void add(ShoppingCartDTO shoppingCartDTO) {
        log.info("添加购物车，接收到的DTO: {}", shoppingCartDTO);
        Long userId = BaseContext.getCurrentId();
        log.info("当前用户ID: {}", userId);

        ShoppingCart shoppingCart = new ShoppingCart();
        BeanUtils.copyProperties(shoppingCartDTO, shoppingCart);
        shoppingCart.setUserId(userId);

        log.info("查询购物车列表，查询条件: {}", shoppingCart);
        List<ShoppingCart> list = shoppingCartMapper.list(shoppingCart);
        log.info("查询结果数量: {}, 结果: {}", list == null ? 0 : list.size(), list);

        if (list != null && !list.isEmpty()) {
            ShoppingCart cart = list.get(0);
            log.info("购物车中已存在该商品，原数量: {}", cart.getNumber());
            cart.setNumber(cart.getNumber() + 1);
            shoppingCartMapper.updateNumberById(cart);
            log.info("更新后数量: {}", cart.getNumber());
        } else {
            log.info("购物车中不存在该商品，准备新增");
            Long dishId = shoppingCartDTO.getDishId();
            if (dishId != null) {
                log.info("添加的是菜品，菜品ID: {}", dishId);
                Dish dish = dishMapper.getById(dishId);
                log.info("查询到的菜品信息: {}", dish);
                if (dish == null) {
                    log.error("菜品不存在，菜品ID: {}", dishId);
                    throw new RuntimeException("菜品不存在");
                }
                shoppingCart.setName(dish.getName());
                shoppingCart.setImage(dish.getImage());
                shoppingCart.setAmount(dish.getPrice());
                log.info("设置菜品信息 - 名称: {}, 价格: {}, 图片: {}", dish.getName(), dish.getPrice(), dish.getImage());
            } else {
                Long setmealId = shoppingCartDTO.getSetmealId();
                log.info("添加的是套餐，套餐ID: {}", setmealId);
                Setmeal setmeal = setmealMapper.getById(setmealId);
                log.info("查询到的套餐信息: {}", setmeal);
                if (setmeal == null) {
                    log.error("套餐不存在，套餐ID: {}", setmealId);
                    throw new RuntimeException("套餐不存在");
                }
                shoppingCart.setName(setmeal.getName());
                shoppingCart.setImage(setmeal.getImage());
                shoppingCart.setAmount(setmeal.getPrice());
                log.info("设置套餐信息 - 名称: {}, 价格: {}, 图片: {}", setmeal.getName(), setmeal.getPrice(), setmeal.getImage());
            }
            shoppingCart.setNumber(1);
            shoppingCart.setCreateTime(LocalDateTime.now());
            log.info("准备插入购物车数据: {}", shoppingCart);
            shoppingCartMapper.insert(shoppingCart);
            log.info("购物车数据插入成功");
        }
    }

    @Override
    public List<ShoppingCart> list() {
        Long userId = BaseContext.getCurrentId();
        log.info("查询购物车列表，用户ID: {}", userId);
        ShoppingCart shoppingCart = ShoppingCart.builder()
                .userId(userId).build();
        return shoppingCartMapper.list(shoppingCart);
    }

    @Override
    public void clean() {
        Long userId = BaseContext.getCurrentId();
        log.info("清空购物车，用户ID: {}", userId);
        ShoppingCart shoppingCart = ShoppingCart.builder()
                .userId(userId).build();
        shoppingCartMapper.delete(shoppingCart);
    }

    @Override
    public void sub(ShoppingCartDTO shoppingCartDTO) {
        Long userId = BaseContext.getCurrentId();
        log.info("用户ID: {}", userId);
        ShoppingCart shoppingCart = new ShoppingCart();
        BeanUtils.copyProperties(shoppingCartDTO, shoppingCart);
        shoppingCart.setUserId(userId);
        log.info("查询购物车列表，查询条件: {}", shoppingCart);
        List<ShoppingCart> list = shoppingCartMapper.list(shoppingCart);
        log.info("查询结果数量: {}, 列表: {}", list == null ? 0 : list.size(), list);
        if (list != null && !list.isEmpty()) {
            ShoppingCart cart = list.get(0);
            log.info("购物车中已存在该商品，原数量: {}", cart.getNumber());
            if (cart.getNumber() == 1) {
                log.info("数量为1，从购物车中删除");
                shoppingCartMapper.delete(cart);
            } else {
                log.info("数量大于1，数量减1");
                cart.setNumber(cart.getNumber() - 1);
                shoppingCartMapper.updateNumberById(cart);
            }
            log.info("更新后数量: {}", cart.getNumber());
            log.info("删除购物车成功");
        }

    }
}

