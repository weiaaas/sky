package com.sky.mapper;

import com.sky.entity.SetmealDish;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SetMealDishMapper {

    List<Long> getSetmealDishIds(List<Long> dishId);


    void addSetmealDish(List<SetmealDish> setmealDishes);

    @Delete("delete from setmeal_dish where setmeal_id=#{id}")
    void deleteById(Long id);
}
