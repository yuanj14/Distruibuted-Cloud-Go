package com.czu.product.controller;

import com.czu.domain.Product;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/product")
public class ProductController {

    /**
     * 根据ID获取商品信息
     */
    @GetMapping("/{id}")
    public Product getProductById(@PathVariable Long id) {
        // 模拟数据
        return new Product(id, "商品" + id, 99.9 * id, 100);
    }

    /**
     * 获取所有商品列表
     */
    @GetMapping("/list")
    public List<Product> getAllProducts() {
        return Arrays.asList(
                new Product(1L, "iPhone 15", 5999.0, 100),
                new Product(2L, "MacBook Pro", 12999.0, 50),
                new Product(3L, "AirPods Pro", 1899.0, 200)
        );
    }
}
