package com.sky.service.impl;

import com.sky.constant.MessageConstant;
import com.sky.constant.PasswordConstant;
import com.sky.constant.StatusConstant;
import com.sky.context.BaseContext;
import com.sky.dto.EmployeeDTO;
import com.sky.dto.EmployeeLoginDTO;
import com.sky.dto.EmployeePageQueryDTO;
import com.sky.entity.Employee;
import com.sky.exception.AccountLockedException;
import com.sky.exception.AccountNotFoundException;
import com.sky.exception.PasswordErrorException;
import com.sky.mapper.EmployeeMapper;
import com.sky.result.PageResult;
import com.sky.service.EmployeeService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class EmployeeServiceImpl implements EmployeeService {

    @Autowired
    private EmployeeMapper employeeMapper;
    /**
     * 员工登录
     *
     * @param employeeLoginDTO
     * @return
     */
    public Employee login(EmployeeLoginDTO employeeLoginDTO) {
        String username = employeeLoginDTO.getUsername();
        String password = employeeLoginDTO.getPassword();
        //1、根据用户名查询数据库中的数据
        Employee employee = employeeMapper.getByUsername(username);
        //2、处理各种异常情况（用户名不存在、密码不对、账号被锁定）
        if (employee == null) {
            //账号不存在
            throw new AccountNotFoundException(MessageConstant.ACCOUNT_NOT_FOUND);
        }
        //密码比对
        // TODO 后期需要进行md5加密，然后再进行比对
         password = DigestUtils.md5DigestAsHex(password.getBytes());
        if (!password.equals(employee.getPassword())) {
            //密码错误
            throw new PasswordErrorException(MessageConstant.PASSWORD_ERROR);
        }
        if (employee.getStatus() == StatusConstant.DISABLE) {
            //账号被锁定
            throw new AccountLockedException(MessageConstant.ACCOUNT_LOCKED);
        }
        //3、返回实体对象
        return employee;
    }
    @Override
    public void user(EmployeeDTO employeeDTO) {

        Employee employee=new Employee();
        //拷贝属性
        BeanUtils.copyProperties(employeeDTO,employee);
        //账号状态，1为正常 0韦非正常
        employee.setStatus(StatusConstant.ENABLE);
//        employee.setCreateTime(LocalDateTime.now());
//        employee.setUpdateTime(LocalDateTime.now());
        //默认密码123456 MD5加密
        employee.setPassword(DigestUtils.md5DigestAsHex(PasswordConstant.DEFAULT_PASSWORD.getBytes()));
        employee.setIdNumber("5");
        //TODO 改为用户id
//        Long currentId = BaseContext.getCurrentId();
//        employee.setUpdateUser(currentId);
//        employee.setCreateUser(currentId);
//        BaseContext.removeCurrentId();
        employeeMapper.User(employee);


    }

    @Override
    /*
    分页查询
     */
    public PageResult Select(EmployeePageQueryDTO employeePageQueryDTO) {
        long total=employeeMapper.count();
        Integer pageSize=employeePageQueryDTO.getPageSize();
        Integer start=(employeePageQueryDTO.getPage()-1)*pageSize;
        String name=employeePageQueryDTO.getName();
        List<Employee> list = employeeMapper.Select(name,start,pageSize);
        PageResult pageResult=new PageResult(total,list);
        return pageResult;
    }

    @Override
    /*
     修改员工
     */
    public void Update(EmployeeDTO employeeDTO) {
        Employee employee=new Employee();
        BeanUtils.copyProperties(employeeDTO,employee);
//        employee.setUpdateTime(LocalDateTime.now());
//        Long currentId = BaseContext.getCurrentId();
//        employee.setUpdateUser(currentId);
       employeeMapper.Update(employee);
    }
    /*
    更改员工状态
     */
    public void EnableOrDisableEmployeeAccounts(Integer status, long id){
     employeeMapper.EnableOrDisableEmployeeAccounts(status,id);
    }
    /*
    在修改员工时根据id查询信息显示在前端上
     */
    public Employee selectById(long id){
        Employee employee=employeeMapper.selecById(id);
        return employee;
    }
}
