package com.sky.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.baomidou.mybatisplus.extension.toolkit.Db;
import com.sky.constant.MessageConstant;
import com.sky.constant.PasswordConstant;
import com.sky.constant.StatusConstant;
import com.sky.dto.*;
import com.sky.exception.AccountLockedException;
import com.sky.exception.AccountNotFoundException;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.exception.PasswordErrorException;
import com.sky.mapper.DishFlavorMapper;
import com.sky.mapper.DishMapper;
import com.sky.mapper.EmployeeMapper;
import com.sky.mapper.SetmealDishMapper;
import com.sky.po.Dish;
import com.sky.po.DishFlavor;
import com.sky.po.Employee;
import com.sky.po.SetmealDish;
import com.sky.result.PageResult;
import com.sky.service.DishService;
import com.sky.service.EmployeeService;
import com.sky.vo.DishVO;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;
import org.springframework.web.servlet.FlashMapManager;

import java.util.List;

@Service
public class DishServiceImpl extends ServiceImpl<DishMapper, Dish> implements DishService {

    @Autowired
    private DishMapper dishMapper;

    @Autowired
    private DishFlavorMapper dishFlavorMapper;

    @Autowired
    private SetmealDishMapper setmealDishMapper;

    /**
     *
     * @param dishDTO
     */
    @Override
    public void saveWithFlavor(DishDTO dishDTO) {
        // 添加一条 dish 数据
        Dish dish = BeanUtil.copyProperties(dishDTO, Dish.class);
        dishMapper.insert(dish);

        // 添加 dish 对应的 flavor 集合数据
        List<DishFlavor> flavors = dishDTO.getFlavors();
        if (flavors != null && !flavors.isEmpty()) {
            // 添加 dish 生成 dishId
            for (DishFlavor flavor : flavors) {
                flavor.setDishId(dish.getId());
            }

            dishFlavorMapper.insert(flavors);
        }
//        批处理 性能优化 100k+ data
//        Mapper insert 自带 Batch
//        this.saveBatch(flavors);
//        &rewriteBatchedStatements=true
//        url: jdbc:mysql://127.0.0.1:port/table?&rewriteBatchedStatements=true
    }

    /**
     *
     * @param dishPageQueryDTO
     * @return
     */
    @Override
    public PageResult pageQuery(DishPageQueryDTO dishPageQueryDTO) {
        Page<DishVO> page = new Page<>(dishPageQueryDTO.getPage(), dishPageQueryDTO.getPageSize());

        // 2. MP Lambda拼接查询条件（类型安全，无硬编码）
        LambdaQueryWrapper<Dish> wrapper = new LambdaQueryWrapper<Dish>()
                .like(StringUtils.isNotBlank(dishPageQueryDTO.getName()),
                        Dish::getName,
                        dishPageQueryDTO.getName()
                )
                .eq(dishPageQueryDTO.getCategoryId() != null,
                        Dish::getCategoryId,
                        dishPageQueryDTO.getCategoryId()
                )
                .eq(dishPageQueryDTO.getStatus() != null,
                        Dish::getStatus,
                        dishPageQueryDTO.getStatus()
                )
                .orderByDesc(Dish::getUpdateTime);

        IPage<DishVO> resultPage = dishMapper.pageQuery(page, dishPageQueryDTO, wrapper);
        List<DishVO> dishVOList = resultPage.getRecords();

        return new PageResult(resultPage.getTotal(), dishVOList);
    }

    /**
     *
     * @param ids
     */
    @Override
    @Transactional
    public void deleteByBatch(List<Long> ids) {
        // 起售菜品不能删
        for (Long id : ids) {
            Dish dish = dishMapper.selectById(id);
            if (dish != null
                    && dish.getStatus().equals(StatusConstant.ENABLE)
            ) {
                throw new DeletionNotAllowedException(MessageConstant.DISH_ON_SALE);
            }
        }
        // 套餐内菜品不能删
        List<SetmealDish> setmealDishes = setmealDishMapper.selectList(
                new LambdaQueryWrapper<SetmealDish>()
                        .in(SetmealDish::getDishId, ids)
        );
        if (setmealDishes != null && !setmealDishes.isEmpty()) {
            throw new DeletionNotAllowedException(MessageConstant.DISH_ON_SALE);
        }

        dishMapper.deleteByIds(ids);

        LambdaQueryWrapper<DishFlavor> wrapper = new LambdaQueryWrapper<DishFlavor>()
                .in(DishFlavor::getDishId, ids);
        dishFlavorMapper.deleteByDishIds(wrapper);

    }

    /**
     *
     * @param id
     * @return
     */
    @Override
    public DishVO getByIdWithFlavor(Long id) {
        Dish dish = dishMapper.selectById(id);
        List<DishFlavor> flavors = Db.lambdaQuery(DishFlavor.class).
                eq(DishFlavor::getDishId, id)
                .list();

        DishVO dishVO = BeanUtil.copyProperties(dish, DishVO.class);
        dishVO.setFlavors(flavors);
        return dishVO;
    }

    @Override
    public void updateWithFlavor(DishDTO dishDTO) {
        Dish dish = BeanUtil.copyProperties(dishDTO, Dish.class);

        // 原来的wrapper → 改为dishUpdateWrapper
        LambdaQueryWrapper<Dish> dishUpdateWrapper = new LambdaQueryWrapper<Dish>()
                .eq(Dish::getId, dish.getId());
        dishMapper.update(dish, dishUpdateWrapper);

        LambdaQueryWrapper<DishFlavor> flavorDeleteWrapper = new LambdaQueryWrapper<DishFlavor>()
                .in(DishFlavor::getDishId, dish.getId());
        dishFlavorMapper.deleteByDishIds(flavorDeleteWrapper);

        // 添加 dish 对应的 flavor 集合数据
        List<DishFlavor> flavors = dishDTO.getFlavors();
        if (flavors != null && !flavors.isEmpty()) {
            // 添加 dish 生成 dishId
            for (DishFlavor flavor : flavors) {
                flavor.setDishId(dish.getId());
            }

            dishFlavorMapper.insert(flavors);
        }
    }

}
