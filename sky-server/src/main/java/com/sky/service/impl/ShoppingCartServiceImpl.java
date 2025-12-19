package com.sky.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.toolkit.Db;
import com.sky.context.BaseContext;
import com.sky.dto.ShoppingCartDTO;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.po.Dish;
import com.sky.po.Setmeal;
import com.sky.po.ShoppingCart;
import com.sky.mapper.ShoppingCartMapper;
import com.sky.service.ShoppingCartService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * <p>
 * 购物车 服务实现类
 * </p>
 *
 * @author ruo11
 * @since 2025-12-18
 */
@Service
public class ShoppingCartServiceImpl extends ServiceImpl<ShoppingCartMapper, ShoppingCart> implements ShoppingCartService {

    @Autowired
    private ShoppingCartMapper shoppingCartMapper;

    @Autowired
    private DishMapper dishMapper;

    @Autowired
    private SetmealMapper setmealMapper;

    /**
     *
     * @param shoppingCartDTO
     */
    @Override
    public void addShoppingCart(ShoppingCartDTO shoppingCartDTO) {
        // DTO TO PO
        ShoppingCart shoppingCart = BeanUtil.copyProperties(shoppingCartDTO, ShoppingCart.class);
        Long userId = BaseContext.getCurrentId();
        shoppingCart.setUserId(userId);

        LambdaQueryWrapper<ShoppingCart> queryWrapper = new LambdaQueryWrapper<ShoppingCart>()
                .eq(shoppingCart.getUserId() != null,
                        ShoppingCart::getUserId,
                        shoppingCart.getUserId()
                )
                .eq(shoppingCartDTO.getSetmealId() != null,
                        ShoppingCart::getSetmealId,
                        shoppingCartDTO.getSetmealId()
                )
                .eq(shoppingCartDTO.getDishId() != null,
                        ShoppingCart::getDishId,
                        shoppingCartDTO.getDishId()
                )
                .eq(shoppingCartDTO.getDishFlavor() != null,
                        ShoppingCart::getDishFlavor,
                        shoppingCartDTO.getDishFlavor()
                );
        List<ShoppingCart> list = shoppingCartMapper.list(shoppingCart, queryWrapper);
        // 存在数量 number + 1
        if (list != null && !list.isEmpty()) {
            ShoppingCart cart = list.get(0);
            cart.setNumber(cart.getNumber() + 1);
            shoppingCartMapper.updateById(cart);
//            Db.lambdaUpdate(ShoppingCart.class).eq(ShoppingCart::getId, cart.getId())
//                    .setSql("number = number - 1").update();
        } else {
            // 不存在添加进购物车
            Long dishId = shoppingCartDTO.getDishId();
            if (dishId != null) {
                // 添加项为菜品
                Dish dish = dishMapper.selectById(dishId);
                shoppingCart.setName(dish.getName());
                shoppingCart.setImage(dish.getImage());
                shoppingCart.setAmount(dish.getPrice());
            } else {
                // 添加项为套餐
                Long setmealId = shoppingCartDTO.getSetmealId();
                Setmeal setmeal = setmealMapper.getById(setmealId);
                shoppingCart.setName(setmeal.getName());
                shoppingCart.setImage(setmeal.getImage());
                shoppingCart.setAmount(setmeal.getPrice());
            }
            shoppingCart.setNumber(1);
            shoppingCartMapper.insert(shoppingCart);
        }
    }

    /**
     *
     * @return
     */
    @Override
    public List<ShoppingCart> showShoppingCart() {
        Long userId = BaseContext.getCurrentId();
        List<ShoppingCart> res = shoppingCartMapper.list(null,
                new LambdaQueryWrapper<ShoppingCart>()
                        .eq(ShoppingCart::getUserId, userId)
        );
        return res;
    }

    /**
     *
     */
    @Override
    public void cleanShoppingCart() {
        Long userId = BaseContext.getCurrentId();
        shoppingCartMapper.delete(
               new LambdaQueryWrapper<ShoppingCart>()
                       .eq(ShoppingCart::getUserId, userId)
       );
    }
}
