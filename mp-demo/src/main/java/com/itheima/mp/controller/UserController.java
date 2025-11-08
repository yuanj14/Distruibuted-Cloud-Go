package com.itheima.mp.controller;

import cn.hutool.core.bean.BeanUtil;
import com.itheima.mp.domain.dto.UserFormDTO;
import com.itheima.mp.domain.po.User;
import com.itheima.mp.domain.query.UserQuery;
import com.itheima.mp.domain.vo.UserVO;
import com.itheima.mp.service.IUserService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

//Swagger 接口注解
@Api(tags = "用户管理接口")
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {
    // @autowired
    private final IUserService userService;

    // DTO PO VO
    // DTO data transfer object 前端传过来的数据
    // PO persistent object 持久层对象 corresponding 数据库实例
    // VO view object 业务处理完成之后传给前端的返回值
    @ApiOperation("新增用户接口")
    @PostMapping
    public void saveUser(@RequestBody UserFormDTO userFormDTO){
        // BeanUtil source target return target
        User user = BeanUtil.copyProperties(userFormDTO, User.class);
        userService.save(user);
    }

    @ApiOperation("删除用户接口")
    @DeleteMapping("/{id}")
    public void deleteUserById(@ApiParam("用户id") @PathVariable("id") Integer id){
        userService.removeById(id);
    }

    @ApiOperation("根据id查询用户接口")
    @GetMapping("/{id}")
    public UserVO queryUserById(@ApiParam("用户id") @PathVariable("id") Integer id){
//        User user = userService.getById(id);
//        return BeanUtil.copyProperties(user, UserVO.class);
        return userService.queryUserAndAdressById(id);
    }

    @ApiOperation("根据id批量查询用户接口")
    @GetMapping
    public List<UserVO> queryUserByIds(@ApiParam("用户id集合") @RequestParam("ids") List<Long> ids){
        return userService.queryUserAndAdressByIds(ids);
    }

    @ApiOperation("扣减用户金额接口")
    @PutMapping("/{id}/deduction/{money}")
    public void queryUserByIds(
            @ApiParam("用户id") @PathVariable("id") Long id,
            @ApiParam("扣减金额") @PathVariable("money") Long money){
        userService.deductBalance(id, money);
    }
    // Lambda
    @ApiOperation("复杂条件查询用户接口")
    @GetMapping("/list")
    public List<UserVO> queryUsers(@ApiParam("condition of query") UserQuery userQuery){
        List<User> users = userService.queryUsers(
                userQuery.getName(),
                userQuery.getStatus(),
                userQuery.getMinBalance(),
                userQuery.getMaxBalance()
        );
        return BeanUtil.copyToList(users, UserVO.class);
    }

}
