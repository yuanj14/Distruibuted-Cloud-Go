package com.itheima.mp.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.itheima.mp.domain.po.User;
import com.itheima.mp.domain.vo.UserVO;
import org.springframework.stereotype.Service;

import java.util.List;

public interface IUserService extends IService<User> {

    void deductBalance(Long id, Long money);

    List<User> queryUsers(String name, Integer status, Integer minBalance, Integer maxBalance);

    UserVO queryUserAndAdressById(Integer id);

    List<UserVO> queryUserAndAdressByIds(List<Long> ids);
}
