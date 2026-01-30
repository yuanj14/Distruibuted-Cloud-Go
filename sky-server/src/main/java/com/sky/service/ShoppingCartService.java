package com.sky.service;

import com.sky.dto.ShoppingCartDTO;
import com.sky.po.ShoppingCart;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * <p>
 * 购物车 服务类
 * </p>
 *
 * @author ruo11
 * @since 2025-12-18
 */
public interface ShoppingCartService extends IService<ShoppingCart> {

    void addShoppingCart(ShoppingCartDTO shoppingCartDTO);

    List<ShoppingCart> showShoppingCart();

    void cleanShoppingCart();

}
