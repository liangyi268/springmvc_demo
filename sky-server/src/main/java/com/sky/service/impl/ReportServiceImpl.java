package com.sky.service.impl;

import com.sky.dto.OrderTurnoverQueryDTO;
import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import com.sky.service.ReportService;
import com.sky.vo.TurnoverReportVO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class ReportServiceImpl implements ReportService {

    private final OrderMapper orderMapper;

    public ReportServiceImpl(OrderMapper orderMapper) {
        this.orderMapper = orderMapper;
    }

    /**
     * 营业额统计
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
}
