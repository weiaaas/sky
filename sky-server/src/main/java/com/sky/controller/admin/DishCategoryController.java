package com.sky.controller.admin;

import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.DishCategoryService;
import com.sky.vo.DishVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/admin/dish")
@Api(tags ="菜品相关接口")
@Slf4j
public class DishCategoryController {
    @Autowired
    private DishCategoryService dishCategoryService;
    @Autowired
    private RedisTemplate redisTemplate;
    @PostMapping
    @ApiOperation("新增菜品接口")
    public Result AddDish(@RequestBody DishDTO dishDTO){
        log.info("新增菜品:{}",dishDTO);
        dishCategoryService.addDish(dishDTO);
        String key="dish_"+dishDTO.getCategoryId();
        redisTemplate.delete(key);
        return Result.success();
    }
    @ApiOperation("菜品分页接口")
    @GetMapping("/page")
    public Result<PageResult> Page(DishPageQueryDTO dishPageQueryDTO){
        log.info("查询菜品:{}",dishPageQueryDTO);
        PageResult pageResult =dishCategoryService.Page(dishPageQueryDTO);
        return Result.success(pageResult);
    }
    @ApiOperation("修改菜品")
    @PutMapping
    public Result  update(@RequestBody DishDTO dishDTO){
        log.info("修改菜品:{}",dishDTO);
        dishCategoryService.update(dishDTO);
        //删除全部缓存
        cleanCache("dish_*");
        return Result.success();
    }
    @ApiOperation("根据ID查询菜品")
    @GetMapping("{id}")
    public Result<DishVO> selectById(@PathVariable Integer id){
        log.info("根据ID查询菜品：",id);
        DishVO dishvo= dishCategoryService.selectById(id);
        return Result.success(dishvo);
    }
    @PostMapping("/status/{status}")
    public Result startOrStopDish(@PathVariable Integer status,Integer id){
        log.info("根据Id更改status启动或停售");
        dishCategoryService.startOrStop(status,id);
        if(status==0){
            DishVO dishVO = dishCategoryService.selectById(id);
            cleanCache("dish_"+dishVO.getCategoryId());
        }
        return Result.success();
    }
    @GetMapping("/list")
    public Result selectByCategoryId(Long categoryId){
        log.info("根据分类id查询",categoryId);
        List<Dish> dish= dishCategoryService.selectByCategoryId(categoryId);
        return Result.success(dish);
    }
    @ApiOperation("根据id批量删除数据")
    @DeleteMapping
    public Result deteleById(@RequestParam List<Long> ids){
        log.info("根据id批量删除数据",ids);
        dishCategoryService.deteleById(ids);
        //删除全部缓存
        cleanCache("dish_*");

        return Result.success();
    }

    /**
     *
     * 删除缓存数据
     * @param paramter
     */
    private void cleanCache(String paramter){
        Set keys = redisTemplate.keys(paramter);
        redisTemplate.delete(keys);
    }
}
