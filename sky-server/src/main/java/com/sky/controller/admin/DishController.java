package com.sky.controller.admin;

import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.dto.EmployeePageQueryDTO;
import com.sky.po.Dish;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.DishService;
import com.sky.vo.DishVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;


@RestController
@RequestMapping("/admin/dish")
@Api(tags = "菜品相关接口")
@Slf4j
public class DishController {

    @Autowired
    private DishService dishService;

    @Autowired
    private RedisTemplate redisTemplate;

    /**
     *
     * @param dishDTO
     * @return
     */
    @ApiOperation("新增菜品")
    @PostMapping
    @Transactional(rollbackFor = Exception.class)
    public Result save(@RequestBody DishDTO dishDTO){
        dishService.saveWithFlavor(dishDTO);

        // 清空缓存
        String key = "dish_" + dishDTO.getCategoryId();
        cleanRedis(key);

        return Result.success();
    }

    /**
     *
     * @param dishPageQueryDTO
     * @return
     */
    @ApiOperation("菜品分页查询接口")
    @GetMapping("/page")
    // Multi query param @ModelAttribute
    public Result<PageResult> page(@ModelAttribute DishPageQueryDTO dishPageQueryDTO) {
        PageResult pageResult = dishService.pageQuery(dishPageQueryDTO);
        return Result.success(pageResult);
    }

    /**
     *
     * @param ids
     * @return
     */
    @ApiOperation("批量删除菜品接口")
    @DeleteMapping
    public Result delete(@RequestParam List<Long> ids){
        dishService.deleteByBatch(ids);

        //将所有的菜品缓存数据清理 dish_*
        cleanRedis("dish_*");

        return Result.success();
    }


    @GetMapping("/{id}")
    @ApiOperation("根据id查询菜品")
    public Result<DishVO> getById(@PathVariable Long id){
        DishVO dishVO = dishService.getByIdWithFlavor(id);
        return Result.success(dishVO);
    }

    @PutMapping
    @ApiOperation("修改菜品")
    public Result update(@RequestBody DishDTO dishDTO){
        dishService.updateWithFlavor(dishDTO);
        //将所有的菜品缓存数据清理 dish_*
        cleanRedis("dish_*");

        return Result.success();
    }

    @GetMapping("/list")
    @ApiOperation("根据分类id查询菜品")
    public Result<List<Dish>> list(Long categoryId){
        List<Dish> list = dishService.list(categoryId);
        return Result.success(list);
    }

    /**
     * 菜品起售停售
     * @param status
     * @param id
     * @return
     */
    @PostMapping("/status/{status}")
    @ApiOperation("菜品起售停售")
    public Result<String> startOrStop(@PathVariable Integer status, Long id) {
        dishService.startOrStop(status, id);

        //将所有的菜品缓存数据清理 dish_*
        cleanRedis("dish_*");

        return Result.success();
    }

    // common method in this class
    private void cleanRedis(String pattern){
        Set keys = redisTemplate.keys(pattern);
        redisTemplate.delete(keys);
    }
}