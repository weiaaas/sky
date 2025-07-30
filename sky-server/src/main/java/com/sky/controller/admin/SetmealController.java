package com.sky.controller.admin;

import com.sky.dto.SetmealDTO;
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.impl.SetmealServiceImpl;
import com.sky.vo.SetmealVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/setmeal")
@Slf4j
@Api(tags = "套餐相关")
public class SetmealController {
    @Autowired
    private SetmealServiceImpl setmealService;
    /**
     * 修改套餐
     * @return
     */
    @PutMapping("")
    @ApiOperation("修改套餐接口")
    public Result updateSetmeal(@RequestBody SetmealDTO setmealDTO){
        log.info("修改套餐{}",setmealDTO);
        setmealService.updateSetmeal(setmealDTO);
        return Result.success();
    }

    /**
     *
     * 新增套餐
     * @param setmealDTO
     * @return
     */
    @ApiOperation("新增套餐接口")
    @PostMapping("")
    @CacheEvict(cacheNames = "setmealCache",key = "#setmealDTO.categoryId")
    public Result addSetmeal(@RequestBody SetmealDTO setmealDTO){
        log.info("新增套餐:{}",setmealDTO);
        setmealService.addSetmeal(setmealDTO);
        return Result.success();
    }
    @ApiOperation("查询套餐接口")
    @GetMapping("/page")
    public Result page(SetmealPageQueryDTO setmealPageQueryDTO){
        log.info("分页查询{}",setmealPageQueryDTO);
        PageResult pageResult =setmealService.page(setmealPageQueryDTO);
        return Result.success(pageResult);
    }

    /**
     *
     * @param id
     * @return
     */
    @ApiOperation("根据id查询套餐")
    @GetMapping("/{id}")
    public Result selectById(@PathVariable Long id){
        log.info("根据id查询套餐{}",id);
        SetmealVO setmealVO= setmealService.selectById(id);
        return Result.success(setmealVO);
    }

    @ApiOperation("起售停售套餐")
    @PostMapping("/status/{status}")
    @CacheEvict(cacheNames = "setmealCache",key = "#id")
    public Result updateStatus(@PathVariable Integer status,Integer id){
        log.info("起售停售套餐",status);
        setmealService.updateStatus(status,id);
        return Result.success();
    }
    @ApiOperation("批量删除套餐")
    @DeleteMapping("")
    @CacheEvict(cacheNames = "setmealCache",allEntries = true)
    public Result deleteById(@RequestParam List<Long> ids){
       log.info("批量删除套餐",ids);
       setmealService.deleteById(ids);
       return Result.success();
    }

}
