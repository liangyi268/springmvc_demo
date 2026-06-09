package com.sky.controller.admin;

import com.sky.result.Result;
import com.sky.service.WorkSpaceService;
import com.sky.vo.BusinessDataVO;
import com.sky.vo.DishOverViewVO;
import com.sky.vo.OrderOverViewVO;
import com.sky.vo.SetmealOverViewVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/workspace")
@Api(tags = "工作台相关接口")
@Slf4j
public class WorkSpaceController {

    @Autowired
    private WorkSpaceService workSpaceService;

    /**
     * 获取今日运营数据
     * @return
     */
    @GetMapping("businessData")
    @ApiOperation("获取今日运营数据")
    public Result<BusinessDataVO> getBusinessData(){
        log.info("获取今日运营数据");
        return Result.success(workSpaceService.getBusinessData());
    }

    /**
     * 获取订单 Overview 数据
     * @return
     */
    @GetMapping("/overviewOrders")
    @ApiOperation("获取订单 Overview 数据")
    public Result<OrderOverViewVO> getOrderOverView(){
        log.info("获取订单 Overview 数据");
        return Result.success(workSpaceService.getOrderOverView());
    }

    /**
     * 获取菜品 Overview 数据
     * @return
     */
    @GetMapping("/overviewDishes")
    @ApiOperation("获取菜品 Overview 数据")
    public Result<DishOverViewVO> getDishOverView(){
        log.info("获取菜品 Overview 数据");
        return Result.success(workSpaceService.getDishOverView());
    }

    @GetMapping("/overviewSetmeals")
    @ApiOperation("获取套餐 Overview 数据")
    public Result<SetmealOverViewVO> getSetmealOverView(){
        log.info("获取套餐 Overview 数据");
        return Result.success(workSpaceService.getSetmealOverView());
    }
}
