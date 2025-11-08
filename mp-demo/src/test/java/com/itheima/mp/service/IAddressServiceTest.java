package com.itheima.mp.service;

import com.itheima.mp.domain.po.User;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class IAddressServiceTest {
    @Autowired
    private IAddressService addressService;

    @Autowired
    private IUserService userService;
    @Test
    void removeAddressById() {
        addressService.removeById(59);
    }

    @Test
    void testService() {
        List<User> list = userService.list();
        list.forEach(System.out::println);
    }

}