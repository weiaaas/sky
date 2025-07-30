package com.sky.service.impl;

import com.sky.constant.MessageConstant;
import com.sky.constant.StatusConstant;
import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.entity.DishFlavor;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.mapper.DishFlavorMapper;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetMealDishMapper;
import com.sky.result.PageResult;
import com.sky.service.DishCategoryService;
import com.sky.vo.DishVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class DishCategoryImpl implements DishCategoryService {
   @Autowired DishMapper dishMapper;
   @Autowired
   DishFlavorMapper dishFlavorMapper;
   @Autowired
   private SetMealDishMapper setMealDishMapper;

    /**
     * 新增菜品和对应的口味数据
     * @param dishDTO
     */
    @Transactional
    public void addDish(DishDTO dishDTO){
        Dish dish=new Dish();
        BeanUtils.copyProperties(dishDTO,dish);
        dishMapper.addDish(dish);
        //获取insert语句生成的主键值
        Long id = dish.getId();
        //添加一个菜品数据
        //  向口味表添加n条数据
        List<DishFlavor> dishFlavorList = dishDTO.getFlavors();

        if(dishFlavorList!=null&&dishFlavorList.size()>0){
            dishFlavorList.forEach(dishFlavor -> {
                dishFlavor.setDishId(id);
            });
            dishFlavorMapper.addDishFlavor(dishFlavorList);
        }
    }
    public PageResult Page(DishPageQueryDTO dishPageQueryDTO){
        Integer pageSize=dishPageQueryDTO.getPageSize();
        Integer page = dishPageQueryDTO.getPage();
        Integer start=(page-1)*pageSize;
        String name=dishPageQueryDTO.getName();
        Integer categoryId=dishPageQueryDTO.getCategoryId();
        Integer status=dishPageQueryDTO.getStatus();
        List<DishVO> dish= dishMapper.Page(name,pageSize,start,categoryId,status);
        Long count=dishMapper.count();
        PageResult pageResult=new PageResult(count,dish);
        return pageResult;
    }

    /**
     * 修改菜品
     * @param dishDTO
     */
    public void update(DishDTO dishDTO){
        Dish dish=new Dish();
        BeanUtils.copyProperties(dishDTO,dish);
        List<DishFlavor> flavors = dishDTO.getFlavors();
        Long dishId=dishDTO.getId();
        dishMapper.updateDish(dish);
        dishFlavorMapper.deteleByDishId(dishId);  //将原有口味删除
        dishMapper.updateFlavor(flavors);
        if(flavors!=null&&flavors.size()>0){
            dishMapper.updateFlavor(flavors);
        }

    }
    public DishVO selectById(Integer id){
        DishDTO dishdto= dishMapper.selectById(id);
        String categoryName= dishMapper.selectCategoryNameByid(id);
        List<DishFlavor> dishFlavorList = dishMapper.selectFlavorById(id);
        DishVO dishVo=new DishVO();
        BeanUtils.copyProperties(dishdto,dishVo);
        dishVo.setCategoryName(categoryName);
        dishVo.setFlavors(dishFlavorList);
        dishVo.setFlavors(dishFlavorList) ;
        return dishVo;
    }
    public void startOrStop(Integer status,Integer id){
        dishMapper.startOrStop(status,id);
    }
    public List<Dish> selectByCategoryId(Long categoryId){
        Dish dish=new Dish();
        dish.setCategoryId(categoryId);
        List<Dish> dishList= dishMapper.selectByCategoryId(dish);
        return dishList;
    }
    @Transactional
    public void deteleById(List<Long> ids){
        /**
         * 判断当前菜品是否能删除  起售中不能
         * 有套餐关联不能删除
         * 要连带吧口味数据也删了
         */

        //判断是否在售
        for (Long id : ids) {
            Dish dish= dishMapper.getById(id);
            if(dish.getStatus()== StatusConstant.ENABLE){
               throw new DeletionNotAllowedException(MessageConstant.DISH_ON_SALE);
            }
        }
        //判断是否关联套餐
        List<Long> setmealDishIds = setMealDishMapper.getSetmealDishIds(ids);
        if(setmealDishIds!=null&&setmealDishIds.size()>0){
            //当前菜品被套餐关联
            throw new DeletionNotAllowedException(MessageConstant.DISH_BE_RELATED_BY_SETMEAL);
        }
        //删除菜品表中菜品数据
        for (Long id : ids) {
            dishMapper.deteleById(id);
            //删除关联的口味数据
            dishFlavorMapper.deteleByDishId(id);
        }
    }


}
