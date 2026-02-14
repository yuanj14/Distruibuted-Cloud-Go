package com.czu.order.feign;

import com.czu.domain.Product;
import com.czu.order.feign.fallback.ProductFeignClientFallback;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

/**
 * Product 服务 Feign 客户端
 */
@FeignClient(name = "service-product", fallback = ProductFeignClientFallback.class)
public interface ProductFeignClient {

    /**
     * 根据ID获取商品信息
     */
    @GetMapping("/{id}")
    Product getProductById(@PathVariable("id") Long id);
}
