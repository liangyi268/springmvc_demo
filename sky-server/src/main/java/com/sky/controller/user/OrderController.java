package com.sky.controller.user;

import com.sky.dto.OrdersPageQueryDTO;
import com.sky.dto.OrdersPaymentDTO;
import com.sky.dto.OrdersSubmitDTO;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.OrderService;
import com.sky.vo.OrderPaymentVO;
import com.sky.vo.OrderSubmitVO;
import com.sky.vo.OrderVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController("userOrderController")
@RequestMapping("/user/order")
@Api(tags = "C端订单接口")
@Slf4j
public class OrderController {

    @Autowired
    private OrderService orderService;
    /**
     * 用户下单
     * @param ordersSubmitDTO
     * @return
     */
    @PostMapping("submit")
    @ApiOperation("用户下单")
    public Result<OrderSubmitVO> submit(@RequestBody OrdersSubmitDTO ordersSubmitDTO) {
        log.info("用户下单：{}", ordersSubmitDTO);
        OrderSubmitVO orderSubmitVO=orderService.submit(ordersSubmitDTO);
        return Result.success(orderSubmitVO);
    }

    /**
     * (模拟)订单支付
     * @param ordersPaymentDTO
     * @return
     */
    @PutMapping("payment")
    @ApiOperation("模拟支付（跳过真实支付）")
    public Result payment(@RequestBody OrdersPaymentDTO ordersPaymentDTO) {
        log.info("模拟支付：{}", ordersPaymentDTO);
        orderService.payment(ordersPaymentDTO);
        return Result.success();
    }

    /**
     * 历史订单查询
     * @param ordersPageQueryDTO
     * @return
     */
    @GetMapping("historyOrders")
    @ApiOperation("查看历史订单")
    public Result<PageResult> list(OrdersPageQueryDTO ordersPageQueryDTO) {
        log.info("查看历史订单：{}", ordersPageQueryDTO);
        PageResult pageResult = orderService.list(ordersPageQueryDTO);
        log.info("历史订单：{}", pageResult);
        return Result.success(pageResult);
    }

    /**
     * 订单详情
     * @param id
     * @return
     */
    @GetMapping("orderDetail/{id}")
    @ApiOperation("订单详情")
    public Result<OrderVO> getOrderDetail(@PathVariable Long id) {
        log.info("订单详情，订单id为：{}", id);
        OrderVO orderVO = orderService.getOrderDetail(id);
        return Result.success(orderVO);
    }
    /**
     * 取消订单
     * @param id
     * @return
     */
    @PutMapping("cancel/{id}")
    @ApiOperation("取消订单")
    public Result cancel(@PathVariable Long id) {
        log.info("取消订单，订单id为：{}", id);
        orderService.cancel(id);
        return Result.success();
    }

    @PostMapping("repetition/{id}")
    @ApiOperation("再来一单")
    public Result repetition(@PathVariable Long id) {
        log.info("再来一单，订单id为：{}", id);
        orderService.repetition(id);
        return Result.success();
    }
}
