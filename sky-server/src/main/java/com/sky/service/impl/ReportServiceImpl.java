package com.sky.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.sky.dto.GoodsSalesDTO;
import com.sky.mapper.OrderDetailMapper;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.UserMapper;
import com.sky.po.OrderDetail;
import com.sky.po.Orders;
import com.sky.po.User;
import com.sky.service.ReportService;
import com.sky.vo.OrderReportVO;
import com.sky.vo.SalesTop10ReportVO;
import com.sky.vo.TurnoverReportVO;
import com.sky.vo.UserReportVO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 统计报表Service实现类
 */
@Service
@Slf4j
public class ReportServiceImpl implements ReportService {

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private OrderDetailMapper orderDetailMapper;

    /**
     * 统计指定时间区间内的营业额数据
     * @param begin 开始日期
     * @param end 结束日期
     * @return 营业额报表VO
     */
    @Override
    public TurnoverReportVO getTurnoverStatistics(LocalDate begin, LocalDate end) {
        // 存放从begin到end范围内每天的日期
        List<LocalDate> dateList = getDateList(begin, end);

        // 存放每天的营业额
        List<String> turnoverList = new ArrayList<>();

        for (LocalDate date : dateList) {
            // 查询date日期对应的营业额数据（状态为"已完成"的订单金额合计）
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);

            // 使用 MyBatis Plus 查询已完成订单
            List<Orders> ordersList = orderMapper.selectList(
                    new LambdaQueryWrapper<Orders>()
                            .eq(Orders::getStatus, Orders.COMPLETED)
                            .ge(Orders::getOrderTime, beginTime)
                            .le(Orders::getOrderTime, endTime)
            );

            // 计算当天营业额
            BigDecimal turnover = ordersList.stream()
                    .map(Orders::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            turnoverList.add(turnover.toString());
        }

        return TurnoverReportVO.builder()
                .dateList(StringUtils.join(dateList, ","))
                .turnoverList(StringUtils.join(turnoverList, ","))
                .build();
    }

    /**
     * 统计指定时间区间内的用户数据
     * @param begin 开始日期
     * @param end 结束日期
     * @return 用户报表VO
     */
    @Override
    public UserReportVO getUserStatistics(LocalDate begin, LocalDate end) {
        // 存放从begin到end范围内每天的日期
        List<LocalDate> dateList = getDateList(begin, end);

        // 存放每天的新增用户数量
        List<Integer> newUserList = new ArrayList<>();
        // 存放每天的总用户数量
        List<Integer> totalUserList = new ArrayList<>();

        for (LocalDate date : dateList) {
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);

            // 统计截止到当天的总用户数
            Long totalUser = userMapper.selectCount(
                    new LambdaQueryWrapper<User>()
                            .le(User::getCreateTime, endTime)
            );

            // 统计当天的新增用户数
            Long newUser = userMapper.selectCount(
                    new LambdaQueryWrapper<User>()
                            .ge(User::getCreateTime, beginTime)
                            .le(User::getCreateTime, endTime)
            );

            totalUserList.add(totalUser.intValue());
            newUserList.add(newUser.intValue());
        }

        return UserReportVO.builder()
                .dateList(StringUtils.join(dateList, ","))
                .totalUserList(StringUtils.join(totalUserList, ","))
                .newUserList(StringUtils.join(newUserList, ","))
                .build();
    }

    /**
     * 统计指定时间区间内的订单数据
     * @param begin 开始日期
     * @param end 结束日期
     * @return 订单报表VO
     */
    @Override
    public OrderReportVO getOrderStatistics(LocalDate begin, LocalDate end) {
        // 存放从begin到end范围内每天的日期
        List<LocalDate> dateList = getDateList(begin, end);

        // 存放每天的订单总数
        List<Integer> orderCountList = new ArrayList<>();
        // 存放每天的有效订单数
        List<Integer> validOrderCountList = new ArrayList<>();

        for (LocalDate date : dateList) {
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);

            // 查询每天的订单总数
            Long orderCount = orderMapper.selectCount(
                    new LambdaQueryWrapper<Orders>()
                            .ge(Orders::getOrderTime, beginTime)
                            .le(Orders::getOrderTime, endTime)
            );

            // 查询每天的有效订单数（已完成的订单）
            Long validOrderCount = orderMapper.selectCount(
                    new LambdaQueryWrapper<Orders>()
                            .eq(Orders::getStatus, Orders.COMPLETED)
                            .ge(Orders::getOrderTime, beginTime)
                            .le(Orders::getOrderTime, endTime)
            );

            orderCountList.add(orderCount.intValue());
            validOrderCountList.add(validOrderCount.intValue());
        }

        // 计算时间区间内的订单总数量
        Integer totalOrderCount = orderCountList.stream().reduce(Integer::sum).orElse(0);
        // 计算时间区间内的有效订单数量
        Integer validOrderCount = validOrderCountList.stream().reduce(Integer::sum).orElse(0);
        // 计算订单完成率
        Double orderCompletionRate = 0.0;
        if (totalOrderCount != 0) {
            orderCompletionRate = validOrderCount.doubleValue() / totalOrderCount;
        }

        return OrderReportVO.builder()
                .dateList(StringUtils.join(dateList, ","))
                .orderCountList(StringUtils.join(orderCountList, ","))
                .validOrderCountList(StringUtils.join(validOrderCountList, ","))
                .totalOrderCount(totalOrderCount)
                .validOrderCount(validOrderCount)
                .orderCompletionRate(orderCompletionRate)
                .build();
    }

    /**
     * 统计指定时间区间内的销量排名前10
     * @param begin 开始日期
     * @param end 结束日期
     * @return 销量排名报表VO
     */
    @Override
    public SalesTop10ReportVO getSalesTop10(LocalDate begin, LocalDate end) {
        LocalDateTime beginTime = LocalDateTime.of(begin, LocalTime.MIN);
        LocalDateTime endTime = LocalDateTime.of(end, LocalTime.MAX);

        // 调用Mapper方法查询销量排名
        List<GoodsSalesDTO> salesTop10 = orderMapper.getSalesTop10(beginTime, endTime);

        // 提取商品名称列表
        List<String> names = salesTop10.stream()
                .map(GoodsSalesDTO::getName)
                .toList();
        String nameList = StringUtils.join(names, ",");

        // 提取销量列表
        List<Integer> numbers = salesTop10.stream()
                .map(GoodsSalesDTO::getNumber)
                .toList();
        String numberList = StringUtils.join(numbers, ",");

        return SalesTop10ReportVO.builder()
                .nameList(nameList)
                .numberList(numberList)
                .build();
    }

    /**
     * 获取日期列表
     * @param begin 开始日期
     * @param end 结束日期
     * @return 日期列表
     */
    private List<LocalDate> getDateList(LocalDate begin, LocalDate end) {
        List<LocalDate> dateList = new ArrayList<>();
        dateList.add(begin);
        while (!begin.equals(end)) {
            begin = begin.plusDays(1);
            dateList.add(begin);
        }
        return dateList;
    }
}
