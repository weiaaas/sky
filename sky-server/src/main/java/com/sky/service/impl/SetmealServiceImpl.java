package com.sky.service.impl;

import com.sky.constant.MessageConstant;
import com.sky.constant.StatusConstant;
import com.sky.dto.SetmealDTO;
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.entity.Setmeal;
import com.sky.entity.SetmealDish;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.mapper.SetMealDishMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.result.PageResult;
import com.sky.vo.DishItemVO;
import com.sky.vo.SetmealVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class SetmealServiceImpl implements com.sky.service.SetmealService {
    @Autowired
    private SetmealMapper setmealMapper;
    @Autowired
    private SetMealDishMapper setMealDishMapper;

    @Transactional  //开启事务  删除关联菜品在新增
    public void updateSetmeal(SetmealDTO setmealDTO){
        SetmealVO setmealVO=new SetmealVO();
        BeanUtils.copyProperties(setmealDTO,setmealVO);
        setmealVO.setUpdateTime(LocalDateTime.now());
        setmealMapper.updateSetmeal(setmealVO);

        Long id = setmealVO.getId();
        setMealDishMapper.deleteById(id);
        List<SetmealDish> setmealDishes = setmealVO.getSetmealDishes();
        setmealDishes.forEach(setmealDish -> {
            setmealDish.setSetmealId(id);
        });
        setMealDishMapper.addSetmealDish(setmealDishes);

    }
    @Transactional  //开启事务
    public void addSetmeal(SetmealDTO setmealDTO){
        Setmeal setmeal=new Setmeal();
        BeanUtils.copyProperties(setmealDTO, setmeal);
        setmealMapper.addSetmeal(setmeal);
        Long id = setmeal.getId();  //获取insert后生成的主键

        List<SetmealDish> setmealDishes = setmealDTO.getSetmealDishes();
        if(setmealDishes!=null&&setmealDishes.size()>0){
            setmealDishes.forEach(setmealDish -> {
                setmealDish.setSetmealId(id);
            });
        }
        setMealDishMapper.addSetmealDish(setmealDishes);

    }
    public PageResult page(SetmealPageQueryDTO setmealPageQueryDTO){
        int page = setmealPageQueryDTO.getPage();
        int pageSize = setmealPageQueryDTO.getPageSize();
        String name = setmealPageQueryDTO.getName();
        Integer categoryId = setmealPageQueryDTO.getCategoryId();
        Integer status = setmealPageQueryDTO.getStatus();
        Integer start=(page-1)*pageSize;  //计算起始索引
        List<SetmealVO> setmealVOList= setmealMapper.page(start,pageSize,name,categoryId,status);

        Integer count = setmealMapper.count();
        PageResult pageResult=new PageResult();
        pageResult.setRecords(setmealVOList);
        pageResult.setTotal(count);
        return  pageResult;
    }

    public SetmealVO selectById(Long id) {
        SetmealVO setmealVO = setmealMapper.selectById(id);
        return setmealVO;
    }

    public void updateStatus(Integer status,Integer id) {
        setmealMapper.updateStatus(status,id);
    }

    @Override
    public void deleteById(List<Long> ids) {
        for(Long id:ids){
            SetmealVO setmealVO = setmealMapper.selectById(id);
            if (setmealVO.getStatus()== StatusConstant.ENABLE){
                throw new DeletionNotAllowedException(MessageConstant.DISH_ON_SALE);
            }

        }
        for(Long id:ids){
            setmealMapper.deleteById(id);
            setMealDishMapper.deleteById(id);
        }
    }

    /**
     * 条件查询
     * @param setmeal
     * @return
     */
    public List<Setmeal> list(Setmeal setmeal) {
        List<Setmeal> list = setmealMapper.list(setmeal);
        return list;
    }

    /**
     * 根据id查询菜品选项
     * @param id
     * @return
     */
    public List<DishItemVO> getDishItemById(Long id) {
        return setmealMapper.getDishItemBySetmealId(id);
    }

}
