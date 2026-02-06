package com.czu.product.controller;

import com.czu.domain.Product;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/product")
public class ProductController {

    /**
     * 根据ID获取商品信息
     */
    @GetMapping("/{id}")
    public Product getProductById(@PathVariable Long id) throws InterruptedException {
        // 模拟数据
//        TimeUnit.SECONDS.sleep(10);
        return new Product(id, "商品" + id, 99.9 * id, 100);
    }

}
