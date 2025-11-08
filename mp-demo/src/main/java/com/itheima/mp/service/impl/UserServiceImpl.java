package com.itheima.mp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.baomidou.mybatisplus.extension.toolkit.Db;
import com.itheima.mp.domain.po.Address;
import com.itheima.mp.domain.po.User;
import com.itheima.mp.domain.vo.AddressVO;
import com.itheima.mp.domain.vo.UserVO;
import com.itheima.mp.enums.UserStatus;
import com.itheima.mp.mapper.UserMapper;
import com.itheima.mp.service.IUserService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {
    // 反向校验
    @Override
    public void deductBalance(Long id, Long money) {
        // 1. 查询用户
        User user = getById(id);
        // 2. 校验状态
        if (user == null || user.getStatus() == UserStatus.FROZEN) {
            throw new RuntimeException("用户状态异常!");
        }
        // 3. 校验余额是否充足
        if (user.getBalance() < money) {
            throw new RuntimeException("用户余额不足!");
        }
        // 4. 扣除余额
//        baseMapper.deductBalance(id, money);
        long remainBalance = user.getBalance() - money;
        lambdaUpdate()
                .set(User::getBalance, remainBalance)
                .set(remainBalance == 0, User::getStatus, UserStatus.FROZEN)
                .eq(User::getId, id)
                .eq(User::getBalance, user.getBalance())
                .update();
    }

    @Override
    public List<User> queryUsers(String name, Integer status, Integer minBalance, Integer maxBalance) {
        return lambdaQuery()
                .like(name != null, User::getUsername, name)
                .eq(status != null, User::getStatus, status)
                .ge(minBalance != null, User::getBalance, minBalance)
                .le(maxBalance != null, User::getBalance, maxBalance)
                .list();
    }

    @Override
    public UserVO queryUserAndAdressById(Integer id) {
        User user = getById(id);
        if (user == null || user.getStatus() == UserStatus.FROZEN) {
            throw new RuntimeException("账户异常");
        }
        UserVO userVO = BeanUtil.copyProperties(user, UserVO.class);
        // Address PO
        List<Address> addressPo = Db.lambdaQuery(Address.class).eq(Address::getUserId, id).list();
        if (CollUtil.isNotEmpty(addressPo)) {
            List<AddressVO> addressVo = BeanUtil.copyToList(addressPo, AddressVO.class);
            userVO.setAddress(addressVo);
        }
        return userVO;
    }
    //practice
    @Override
    public List<UserVO> queryUserAndAdressByIds(List<Long> ids) {
        List<User> users = listByIds(ids);
        if (users.isEmpty()) {
            throw new RuntimeException("用户不存在");
        }
        List<UserVO> userVOS = BeanUtil.copyToList(users, UserVO.class);
        userVOS.forEach(userVo -> {
            List<Address> addressPO = Db.lambdaQuery(Address.class).eq(Address::getUserId, userVo.getId()).list();
            userVo.setAddress(BeanUtil.copyToList(addressPO, AddressVO.class));
        });

        return userVOS;
    }


}
