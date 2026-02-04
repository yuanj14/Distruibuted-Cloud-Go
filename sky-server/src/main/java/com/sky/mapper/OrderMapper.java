package com.sky.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sky.dto.GoodsSalesDTO;
import com.sky.po.Orders;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 订单Mapper接口
 */
@Mapper
public interface OrderMapper extends BaseMapper<Orders> {

    /**
     * 根据订单号查询订单
     * @param orderNumber
     */
    @Select("select * from orders where number = #{orderNumber}")
    Orders getByNumber(String orderNumber);

    /**
     * 统计指定时间区间内的销量排名前10
     * @param begin 开始时间
     * @param end 结束时间
     * @return 销量排名列表
     */
    List<GoodsSalesDTO> getSalesTop10(@Param("begin") LocalDateTime begin, @Param("end") LocalDateTime end);

    /**
     * 根据动态条件统计订单数量
     * @param map
     * @return
     */
    Integer countByMap(Map map);

    /**
     * 根据动态条件统计营业额
     * @param map
     * @return
     */
    Double sumByMap(Map map);
}
