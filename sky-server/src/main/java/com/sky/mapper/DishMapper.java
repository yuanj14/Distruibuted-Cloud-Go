package com.sky.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sky.dto.DishPageQueryDTO;
import com.sky.po.Dish;
import com.sky.po.Employee;
import com.sky.vo.DishVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface DishMapper extends BaseMapper<Dish> {

    /**
     * 根据分类id查询菜品数量
     * @param categoryId
     * @return
     */
    @Select("select count(id) from dish where category_id = #{categoryId}")
    Integer countByCategoryId(Long categoryId);

    /**
     * mp分页查询 封装结果对象IPage<E>  E单条数据类型
     * @param page
     * @param dishPageQueryDTO
     * @param queryWrapper
     * @return
     */
    IPage<DishVO> pageQuery(Page<DishVO> page,
                    @Param("dto") DishPageQueryDTO dishPageQueryDTO,
                    @Param(Constants.WRAPPER) LambdaQueryWrapper<Dish> queryWrapper);

}
