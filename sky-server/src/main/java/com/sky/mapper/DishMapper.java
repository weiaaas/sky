package com.sky.mapper;

import com.sky.annotation.AutoFill;
import com.sky.dto.DishDTO;
import com.sky.entity.Dish;
import com.sky.entity.DishFlavor;
import com.sky.enumeration.OperationType;
import com.sky.vo.DishVO;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface DishMapper {

    /**
     * 根据分类id查询菜品数量
     * @param
     * @return
     */
    @Select("select count(id) from dish where category_id = #{categoryId}")
    Integer countByCategoryId(Long categoryId);

    /**
     * 插入菜品
     * @param
     */
    @AutoFill(OperationType.INSERT)
    void addDish(Dish dish);
    @Select("select count(*) from dish")
    Long count();

    List<DishVO> Page(String name, Integer pageSize, Integer start, Integer categoryId,Integer status);

    @AutoFill(OperationType.UPDATE)
    void updateDish(Dish dish);


    void updateFlavor(List<DishFlavor> flavors);

    DishDTO selectById(Integer id);
    @Select("select name from category where id=#{id}")
    String selectCategoryNameByid(Integer id);
    @Select("select id, dish_id, name, value  from dish_flavor where id=#{id}")
    List<DishFlavor> selectFlavorById(Integer id);
    @Update("update dish set status=#{status} where id=#{id}")
    void startOrStop(Integer status, Integer id);
    List<Dish> selectByCategoryId(Dish dish);
    @Select("select * from dish where id=#{id}")
    Dish getById(Long id);
    @Delete("delete from dish where id=#{id}")
    void deteleById(Long id);
}
