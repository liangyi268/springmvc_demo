package com.sky.service.impl;

import com.sky.constant.StatusConstant;
import com.sky.dto.OrderTurnoverQueryDTO;
import com.sky.entity.Orders;
import com.sky.mapper.*;
import com.sky.service.WorkSpaceService;
import com.sky.vo.BusinessDataVO;
import com.sky.vo.DishOverViewVO;
import com.sky.vo.OrderOverViewVO;
import com.sky.vo.SetmealOverViewVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class WorkSpaceServiceImpl implements WorkSpaceService {

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private OrderDetailMapper orderDetailMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private DishMapper dishMapper;

    @Autowired
    private SetmealMapper setmealMapper;

    @Override
    public BusinessDataVO getBusinessData() {
        LocalDateTime localDateTime = LocalDateTime.now();
        LocalDateTime begin = localDateTime.withHour(0).withMinute(0).withSecond(0);
        LocalDateTime end = localDateTime.withHour(23).withMinute(59).withSecond(59);
        OrderTurnoverQueryDTO queryDTO = OrderTurnoverQueryDTO
                .builder()
                .startTime(begin)
                .endTime(end)
                .status(Orders.COMPLETED)
                .build();
        Double turnover = orderMapper.sumByMap(queryDTO);
        Integer validOrderCount = orderMapper.countByDate(queryDTO);
        queryDTO.setStatus(null);
        Integer orderCount = orderMapper.countByDate(queryDTO);
        Double unitPrice = validOrderCount == 0 ? 0.0 : turnover / validOrderCount;
        return BusinessDataVO
                .builder()
                .turnover(turnover)
                .validOrderCount(validOrderCount)
                .orderCompletionRate(validOrderCount.doubleValue() / orderCount)
                .unitPrice(unitPrice)
                .newUsers(userMapper.countByDate(begin, end))
                .build();
    }

    @Override
    public OrderOverViewVO getOrderOverView() {
//     * 订单状态 1待付款 2待接单 3已接单 4派送中 5已完成 6已取消
        return OrderOverViewVO
                .builder()
                .waitingOrders(orderMapper.countByStatus(Orders.TO_BE_CONFIRMED))
                .deliveredOrders(orderMapper.countByStatus(Orders.DELIVERY_IN_PROGRESS))
                .completedOrders(orderMapper.countByStatus(Orders.COMPLETED))
                .cancelledOrders(orderMapper.countByStatus(Orders.CANCELLED))
                .allOrders(orderMapper.countByStatus(null))
                .build();
    }

    @Override
    public DishOverViewVO getDishOverView() {
      Integer sold = dishMapper.countByStatus(StatusConstant.ENABLE);
      Integer discontinued = dishMapper.countByStatus(StatusConstant.DISABLE);
      return DishOverViewVO
              .builder()
              .sold(sold)
              .discontinued(discontinued)
              .build();
    }

    @Override
    public SetmealOverViewVO getSetmealOverView() {
        Integer sold = setmealMapper.countByStatus(StatusConstant.ENABLE);
        Integer discontinued = setmealMapper.countByStatus(StatusConstant.DISABLE);
        return SetmealOverViewVO
                .builder()
                .sold(sold)
                .discontinued(discontinued)
                .build();
    }
}
