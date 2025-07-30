package com.sky.service;

import com.sky.annotation.AutoFill;
import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.result.PageResult;
import com.sky.vo.DishVO;

import java.util.List;

public interface DishCategoryService {
    void addDish(DishDTO dishDTO);

    PageResult Page(DishPageQueryDTO dishPageQueryDTO);

    void update(DishDTO dishDTO);

    DishVO selectById(Integer id);

    void startOrStop(Integer status,Integer id);

    List<Dish> selectByCategoryId(Long categoryId);

    void deteleById(List<Long> ids);


}
