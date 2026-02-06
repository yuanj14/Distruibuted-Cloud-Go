package com.czu.order.feign.fallback;

import com.czu.domain.Product;
import com.czu.order.feign.ProductFeignClient;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
public class ProductFeignClientFallback implements ProductFeignClient {

    @Override
    public Product getProductById(Long id) {
        Product product = new Product();
        product.setId(id);
        product.setName("服务降级-商品不可用");
        product.setPrice(0.0);
        product.setStock(0);
        return product;
    }
}
