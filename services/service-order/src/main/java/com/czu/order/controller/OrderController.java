package com.czu.order.controller;

import com.czu.domain.Product;
import com.czu.order.config.OrderProperties;
import com.czu.order.feign.ProductFeignClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/order")
public class OrderController {

    @Autowired
    private ProductFeignClient productFeignClient;

    @Autowired
    private OrderProperties orderProperties;

    @GetMapping("/config")
    public String config() {
        return  "order timeout : " + orderProperties.getTimeout() + "\n" +
                "order max-retry : " + orderProperties.getMaxRetry();
    }
    /**
     * 下单 - 调用 product 服务获取商品信息
     */
    @GetMapping("/buy/{productId}")
    public String buyProduct(@PathVariable Long productId) {
        Product product = productFeignClient.getProductById(productId);
        return "下单成功！商品：" + product.getName() + "，价格：" + product.getPrice();
    }

    /**
     * 查看可购买商品列表
     */
    @GetMapping("/products")
    public List<Product> getProducts() {
        return productFeignClient.getAllProducts();
    }
}
