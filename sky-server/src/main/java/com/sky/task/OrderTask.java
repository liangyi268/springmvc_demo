package com.sky.task;

import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单超时取消任务
 */
@Component
@Slf4j
public class OrderTask {

    @Autowired
    private OrderMapper orderMapper;
    /**
     * 处理支付订单超时取消任务
     */
    @Scheduled(cron = "0 * * * * ?")    //每分钟执行一次
    public void processTimeoutOrder(){
        log.info("处理订单超时取消任务{}", LocalDateTime.now());
        LocalDateTime time = LocalDateTime.now().plusMinutes(-15);
        List<Orders> ordersList = orderMapper.getByStatusAndOrderTimeLT(Orders.PENDING_PAYMENT, time);
        if (ordersList != null && !ordersList.isEmpty()){
            for (Orders orders : ordersList){
                orderMapper.update(
                        Orders.builder()
                                .id(orders.getId())
                                .status(Orders.CANCELLED)
                                .cancelReason("订单超时取消")
                                .cancelTime(LocalDateTime.now())
                                .build());
            }
        }

    }

    /**
     * 处理一直处于派送中状态的订单
     */
    @Scheduled(cron = "0 0 1 * * ?")
    public void processDeliveryOrder(){
        log.info("处理处于派送中状态的订单{}", LocalDateTime.now());
        LocalDateTime time = LocalDateTime.now().plusMinutes(-60);
        List<Orders> ordersList = orderMapper.getByStatusAndOrderTimeLT(Orders.DELIVERY_IN_PROGRESS, time);
        if (ordersList != null && !ordersList.isEmpty()){
            for (Orders orders : ordersList){
                orderMapper.update(
                        Orders.builder()
                        .id(orders.getId())
                        .status(Orders.COMPLETED)
                        .deliveryTime(LocalDateTime.now())
                        .build()
                );
            }
        }
    }
}
