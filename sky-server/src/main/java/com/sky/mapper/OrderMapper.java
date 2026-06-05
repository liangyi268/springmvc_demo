package com.sky.mapper;

import com.github.pagehelper.Page;
import com.sky.dto.OrdersPageQueryDTO;
import com.sky.entity.Orders;
import com.sky.vo.OrderStatisticsVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface OrderMapper {
    /**
     * 插入订单数据
     * @param orders
     */
    void insert(Orders orders);

    Orders getByNumber(String orderNumber);

    void update(Orders order);

    Page<Orders> list(OrdersPageQueryDTO ordersPageQueryDTO);

    Orders getById(Long id);
    @Select("select * from orders")
    List<Orders> ordersList();

    @Select("select * from sky_take_out.orders where status=#{status} and order_time<#{orderTime}")
    List<Orders>getByStatusAndOrderTimeLT(Integer status, LocalDateTime orderTime);

    @Update("update sky_take_out.orders set status=#{orderStatus},pay_status=#{orderPaidStatus},checkout_time=#{checkOutTime} where number=#{orderNumber}")
    void updateStatus(Integer orderStatus, Integer orderPaidStatus, LocalDateTime checkOutTime, String orderNumber);
}
