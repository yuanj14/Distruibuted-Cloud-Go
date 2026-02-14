package com.czu.product.controller;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.czu.domain.Product;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

@RestController
public class ProductController {

    @SentinelResource(value = "getProductById", blockHandler = "getProductByIdBlockHandler")
    @GetMapping("/{id}")
    public Product getProductById(@PathVariable Long id) {
        return new Product(id, "商品" + id, 99.9 * id, 100);
    }

    public Product getProductByIdBlockHandler(Long id, BlockException ex) {
        return new Product(id, "限流商品", 0.0, 0);
    }

    @GetMapping("/list")
    public List<Product> getAllProducts() {
        return Arrays.asList(
                new Product(1L, "商品1", 99.9, 100),
                new Product(2L, "商品2", 199.9, 50),
                new Product(3L, "商品3", 299.9, 30)
        );
    }
}
