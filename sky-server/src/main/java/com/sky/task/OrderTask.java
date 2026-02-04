package com.sky.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.sky.mapper.OrderMapper;
import com.sky.po.Orders;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单定时任务
 */
@Component
@Slf4j
public class OrderTask {

    @Autowired
    private OrderMapper orderMapper;

    /**
     * 处理超时订单
     * 每分钟触发一次，取消下单超过15分钟仍未支付的订单
     */
    @Scheduled(cron = "0 * * * * ?")
    public void processTimeoutOrder() {
        log.info("定时处理超时订单：{}", LocalDateTime.now());

        // 计算超时时间点（当前时间 - 15分钟）
        LocalDateTime time = LocalDateTime.now().minusMinutes(15);

        // 查询待付款且超时的订单
        LambdaQueryWrapper<Orders> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Orders::getStatus, Orders.PENDING_PAYMENT)
                    .lt(Orders::getOrderTime, time);

        List<Orders> ordersList = orderMapper.selectList(queryWrapper);

        if (ordersList != null && !ordersList.isEmpty()) {
            for (Orders orders : ordersList) {
                // 更新订单状态为已取消
                LambdaUpdateWrapper<Orders> updateWrapper = new LambdaUpdateWrapper<>();
                updateWrapper.eq(Orders::getId, orders.getId())
                             .set(Orders::getStatus, Orders.CANCELLED)
                             .set(Orders::getCancelReason, "订单超时，自动取消")
                             .set(Orders::getCancelTime, LocalDateTime.now());
                orderMapper.update(null, updateWrapper);
            }
            log.info("已取消{}个超时订单", ordersList.size());
        }
    }

    /**
     * 处理派送中订单
     * 每天凌晨1点触发，自动完成上一工作日一直处于派送中状态的订单
     */
    @Scheduled(cron = "0 0 1 * * ?")
    public void processDeliveryOrder() {
        log.info("定时处理派送中订单：{}", LocalDateTime.now());

        // 计算时间点（当前时间 - 1小时），即上一工作日的订单
        LocalDateTime time = LocalDateTime.now().minusMinutes(60);

        // 查询派送中且下单时间在指定时间之前的订单
        LambdaQueryWrapper<Orders> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Orders::getStatus, Orders.DELIVERY_IN_PROGRESS)
                    .lt(Orders::getOrderTime, time);

        List<Orders> ordersList = orderMapper.selectList(queryWrapper);

        if (ordersList != null && !ordersList.isEmpty()) {
            for (Orders orders : ordersList) {
                // 更新订单状态为已完成
                LambdaUpdateWrapper<Orders> updateWrapper = new LambdaUpdateWrapper<>();
                updateWrapper.eq(Orders::getId, orders.getId())
                             .set(Orders::getStatus, Orders.COMPLETED)
                             .set(Orders::getDeliveryTime, LocalDateTime.now());
                orderMapper.update(null, updateWrapper);
            }
            log.info("已完成{}个派送中订单", ordersList.size());
        }
    }
}
