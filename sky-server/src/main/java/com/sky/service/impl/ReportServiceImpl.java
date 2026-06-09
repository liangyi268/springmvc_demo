package com.sky.service.impl;

import com.sky.dto.GoodsSalesDTO;
import com.sky.dto.OrderTurnoverQueryDTO;
import com.sky.entity.Orders;
import com.sky.mapper.OrderDetailMapper;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.UserMapper;
import com.sky.service.ReportService;
import com.sky.service.WorkSpaceService;
import com.sky.vo.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ReportServiceImpl implements ReportService {

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private OrderDetailMapper orderDetailMapper;

    @Autowired
    private WorkSpaceService workSpaceService;

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
        double orderCompletionRate = totalOrderCount == 0 ? 0.0 : validOrderCount * 1.0 / totalOrderCount;
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

    // ... existing code ...
    @Override
    public SalesTop10ReportVO getTop10(LocalDate begin, LocalDate end) {
        log.info("查询销量排名top10：{}到{}", begin, end);

        LocalDateTime startTime = LocalDateTime.of(begin, LocalTime.MIN);
        LocalDateTime endTime = LocalDateTime.of(end, LocalTime.MAX);

        OrderTurnoverQueryDTO queryDTO = OrderTurnoverQueryDTO
                .builder()
                .startTime(startTime)
                .endTime(endTime)
                .status(Orders.COMPLETED)
                .build();

        List<GoodsSalesDTO> resultList = orderDetailMapper.getSalesTop10(queryDTO);
        String names = resultList.stream().map(GoodsSalesDTO::getName).collect(Collectors.joining(","));
        String numbers = resultList.stream().map(GoodsSalesDTO::getNumber).map(Object::toString).collect(Collectors.joining(","));
        log.info("names: {}, numbers: {}", names, numbers);

        return SalesTop10ReportVO
                .builder()
                .nameList(names)
                .numberList(numbers)
                .build();
    }

    /**
     * 导出营业数据
     *
     * @param response
     */
    @Override
    public void exportBusinessData(HttpServletResponse response) {
        //1. 查询营业数据
        LocalDate end = LocalDate.now().minusDays(30);//默认查询30天内的营业数据
        LocalDate begin = LocalDate.now().minusDays(1);//默认查询昨天的数据
        LocalDateTime startTime = LocalDateTime.of(begin, LocalTime.MIN);
        LocalDateTime endTime = LocalDateTime.of(end, LocalTime.MAX);
        BusinessDataVO businessData = workSpaceService.getBusinessData(startTime, endTime);
        //2.通过POI将营业数据写入到Excel中
        InputStream in = this.getClass().getClassLoader().getResourceAsStream("template/运营数据报表模板.xlsx");

        try {
            //基于模板文件创建新的Excel文件
            XSSFWorkbook workbook = new XSSFWorkbook(in);
            //填充数据--获取第一个表单
            XSSFSheet sheet = workbook.getSheet("sheet1");
            //填充数据--时间
            sheet.getRow(1).getCell(1).setCellValue("时间:"+startTime + "至" + endTime);
            //第四行填充数据--营业额
            sheet.getRow(3).getCell(2).setCellValue(businessData.getTurnover());
            sheet.getRow(3).getCell(4).setCellValue(businessData.getOrderCompletionRate());
            sheet.getRow(3).getCell(6).setCellValue(businessData.getNewUsers());
            //第五行填充数据--有效订单数
            sheet.getRow(4).getCell(2).setCellValue(businessData.getValidOrderCount());
            sheet.getRow(4).getCell(4).setCellValue(businessData.getUnitPrice());

            //填充明细数据
            for (int i = 0; i < 30; i++) {
                LocalDate date = begin.minusDays(i);
                //查询某一天的营业数据
                BusinessDataVO businessDataVO = workSpaceService.getBusinessData(
                        LocalDateTime.of(date, LocalTime.MIN), LocalDateTime.of(date, LocalTime.MAX));
                //获得某一行
                XSSFRow row = sheet.getRow(7 + i);
                row.getCell(1).setCellValue(date.toString());
                row.getCell(2).setCellValue(businessDataVO.getTurnover());
                row.getCell(3).setCellValue(businessDataVO.getValidOrderCount());
                row.getCell(4).setCellValue(businessDataVO.getOrderCompletionRate());
                row.getCell(5).setCellValue(businessDataVO.getUnitPrice());
                row.getCell(6).setCellValue(businessDataVO.getNewUsers());
            }

            //3.将Excel写入到浏览器下载
            ServletOutputStream out = response.getOutputStream();
            workbook.write(out);
            out.close();
            workbook.close();
        }catch (IOException e){
            e.printStackTrace();
        }

    }
}

