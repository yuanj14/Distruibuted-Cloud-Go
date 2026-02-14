package com.czu.order.controller;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.czu.domain.Product;
import com.czu.order.config.OrderProperties;
import com.czu.order.feign.ProductFeignClient;
import com.czu.order.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.TimeUnit;

@RestController
public class OrderController {

    @Autowired
    private OrderProperties orderProperties;

    @Autowired
    private OrderService orderService;

    @GetMapping("/config")
    public String config() {
        return  "order timeout : " + orderProperties.getTimeout() + "\n" +
                "order max-retry : " + orderProperties.getMaxRetry();
    }


    @GetMapping("/create/{productId}")
    public Product buyProduct(@PathVariable Long productId) {
        return orderService.createOrder(productId);
    }

    @GetMapping("/seckill/{productId}")
    public Product seckill(@PathVariable Long productId) {
        return orderService.createOrder(productId);
    }

    @SentinelResource(
            value = "hotspot"
    )
    @GetMapping("/hotspot")
    public String hotspot(@RequestParam Long productId, @RequestParam(defaultValue = "normal") String type) {
        return "热点商品访问 - 商品ID: " + productId + ", 类型: " + type;
    }

    @GetMapping("/flow")
    public String flowTest() {
        return "流控测试接口 - " + System.currentTimeMillis();
    }

    @GetMapping("/degrade")
    public String degradeTest(@RequestParam(defaultValue = "false") boolean slow) throws InterruptedException {
        if (slow) {
            TimeUnit.SECONDS.sleep(2);
        }
        return "熔断测试接口 - slow: " + slow;
    }

    @GetMapping("/degrade/error")
    public String degradeErrorTest(@RequestParam(defaultValue = "false") boolean error) {
        if (error) {
            throw new RuntimeException("模拟异常");
        }
        return "熔断异常测试接口";
    }
}
