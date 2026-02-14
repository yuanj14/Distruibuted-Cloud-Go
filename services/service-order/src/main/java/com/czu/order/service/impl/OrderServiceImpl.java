package com.czu.order.service.impl;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.czu.order.feign.ProductFeignClient;
import com.czu.domain.Product;
import com.czu.order.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class OrderServiceImpl implements OrderService {

    @Autowired
    private ProductFeignClient productFeignClient;

    @SentinelResource(
            value = "createOrder",
            blockHandler = "createOrderBlockHandler"
    )
    @Override
    public Product createOrder(Long id) {
        return productFeignClient.getProductById(id);
    }

    // 限流处理方法
    public Product createOrderBlockHandler(Long id, BlockException ex) {
        return new Product(id, "createOrderBlockHandler", 0D, 0);
    }
}
