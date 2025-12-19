package com.sky.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import com.sky.po.Dish;
import com.sky.po.ShoppingCart;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * <p>
 * 购物车 Mapper 接口
 * </p>
 *
 * @author ruo11
 * @since 2025-12-18
 */
@Mapper
public interface ShoppingCartMapper extends BaseMapper<ShoppingCart> {
    List<ShoppingCart> list(
            @Param("shoppingCart") ShoppingCart shoppingCart,
            @Param(Constants.WRAPPER) LambdaQueryWrapper<ShoppingCart> queryWrapper);
}
