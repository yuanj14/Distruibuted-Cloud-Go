package com.sky.service;

import com.sky.dto.OrdersSubmitDTO;
import com.sky.vo.OrderSubmitVO;

/**
 * 订单服务接口
 */
public interface OrderService {

    /**
     * 用户下单
     * @param ordersSubmitDTO 订单提交数据
     * @return 订单提交结果
     */
    OrderSubmitVO submitOrder(OrdersSubmitDTO ordersSubmitDTO);
}
