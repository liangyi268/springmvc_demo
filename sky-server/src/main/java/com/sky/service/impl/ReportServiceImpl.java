package com.sky.service.impl;

import com.sky.dto.OrderTurnoverQueryDTO;
import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.UserMapper;
import com.sky.service.ReportService;
import com.sky.vo.OrderReportVO;
import com.sky.vo.TurnoverReportVO;
import com.sky.vo.UserReportVO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class ReportServiceImpl implements ReportService {

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private UserMapper userMapper;

    /**
     * 营业额统计
     *
     * @param begin
     * @param end
     * @return
     */
    @Override
    public TurnoverReportVO getTurnoverStatistics(LocalDate begin, LocalDate end) {
        log.info("营业额统计：{}到{}", begin, end);
        //当前集合存放日期
        List<LocalDate> dateList = new ArrayList<>();
        dateList.add(begin);
        while (!begin.equals(end)) {
            //日期计算,计算指定日期的后一天的日期
            begin = begin.plusDays(1);
            dateList.add(begin);
        }
        //当前集合存放营业额
        List<Double> turnoverList = new ArrayList<>();
        Integer completed = Orders.COMPLETED;
        for (LocalDate date : dateList) {
            //根据日期查询营业额数据
            LocalDateTime startTime = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);
            OrderTurnoverQueryDTO query = OrderTurnoverQueryDTO
                    .builder()
                    .startTime(startTime)
                    .endTime(endTime)
                    .status(completed)
                    .build();
            Double turnover = orderMapper.sumByMap(query);
            turnoverList.add(turnover == null ? 0.0 : turnover);
        }
        return TurnoverReportVO
                .builder()
                .dateList(StringUtils.join(dateList, ","))
                .turnoverList(StringUtils.join(turnoverList, ","))
                .build();
    }

    /**
     * 用户统计
     *
     * @param begin
     * @param end
     * @return
     */
    @Override
    public UserReportVO getUserStatistics(LocalDate begin, LocalDate end) {
        //存放从begin到end的日期
        List<LocalDate> dateList = new ArrayList<>();
        dateList.add(begin);
        while (!begin.equals(end)) {
            begin = begin.plusDays(1);
            dateList.add(begin);
        }
        //存放从begin到end的日期对应的用户数量
        List<Integer> newUserList = new ArrayList<>();
        //存放从begin到end的日期对应的总用户数量
        List<Integer> totalUserList = new ArrayList<>();
        for (LocalDate date : dateList) {
            LocalDateTime startTime = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);
            newUserList.add(userMapper.countByDate(startTime, endTime));
            startTime = null;
            totalUserList.add(userMapper.countByDate(startTime, endTime));
        }
        return UserReportVO
                .builder()
                .dateList(StringUtils.join(dateList, ","))
                .newUserList(StringUtils.join(newUserList, ","))
                .totalUserList(StringUtils.join(totalUserList, ","))
                .build();
    }

    @Override
    public OrderReportVO getOrdersStatistics(LocalDate begin, LocalDate end) {
        log.info("订单统计：{}到{}", begin, end);
        List<LocalDate> dateList = new ArrayList<>();
        dateList.add(begin);
        while (!begin.equals(end)) {
            begin = begin.plusDays(1);
            dateList.add(begin);
        }
        List<Integer> orderCountList = new ArrayList<>();
        List<Integer> validOrderCountList = new ArrayList<>();
        for (LocalDate date : dateList) {
            //查询订单总数
            LocalDateTime startTime = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);
            OrderTurnoverQueryDTO query = OrderTurnoverQueryDTO
                    .builder()
                    .startTime(startTime)
                    .endTime(endTime)
                    .build();
            Integer count = orderMapper.countByDate(query);
            orderCountList.add(count);
            //查询有效订单数
            query.setStatus(Orders.COMPLETED);
            count = orderMapper.countByDate(query);
            validOrderCountList.add(count);
        }
        Integer totalOrderCount = orderCountList.stream().reduce(Integer::sum).get();
        Integer validOrderCount = validOrderCountList.stream().reduce(Integer::sum).get();
        double orderCompletionRate =  totalOrderCount == 0 ? 0.0 : validOrderCount * 1.0 / totalOrderCount;
        return OrderReportVO
                .builder()
                .dateList(StringUtils.join(dateList, ","))
                .orderCountList(StringUtils.join(orderCountList, ","))
                .validOrderCountList(StringUtils.join(validOrderCountList, ","))
                .totalOrderCount(totalOrderCount)
                .validOrderCount(validOrderCount)
                .orderCompletionRate(orderCompletionRate)
                .build();
    }
}
