package com.czu.order.exception;

import com.alibaba.csp.sentinel.adapter.spring.webmvc_v6x.callback.BlockExceptionHandler;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.czu.domain.common.R;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;

import java.io.PrintWriter;

@Component
/**
 * @Description: 自定义流控异常处理类 WEB 层接口异常处理
 */
public class SentinelBlockExceptionHandler implements BlockExceptionHandler {
    private ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, String resource,
                       BlockException e) throws Exception {
        response.setStatus(429); // Set status code to 429 (Too Many Requests)
        response.setContentType("application/json;charset=utf-8");
        PrintWriter writer = response.getWriter();
        String json = objectMapper.writeValueAsString(R.error(500, "请求过于频繁，请稍后再试"));
        writer.write(json);
        writer.flush();
        writer.close();
    }
}

