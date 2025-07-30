package com.sky.mapper;

import com.sky.annotation.AutoFill;
import com.sky.entity.Employee;
import com.sky.enumeration.OperationType;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface EmployeeMapper {

    /**
     * 根据用户名查询员工
     * @param username
     * @return
     */
    @Select("select * from employee where username = #{username}")
    Employee getByUsername(String username);

    /**
     * 添加员工
     * @param employee
     */
    @AutoFill(value = OperationType.INSERT)
   @Insert("insert into employee (name, username, password, phone, sex, id_number, status, create_time, update_time, create_user, update_user) values " +
           "(#{name},#{username},#{password},#{phone},#{sex},#{idNumber},#{status},#{createTime},#{updateTime},#{createUser},#{updateUser})")
    void User(Employee employee);

    /**
     *
     * @param name
     * @param start
     * @param pageSize
     * @return 分页查询
     */
    List<Employee> Select(@Param("name") String name,@Param("start") Integer start,@Param("pageSize") Integer pageSize);
   @Select("select count(*) from employee")
   long count();

    /**、
     * 更新员工
     * @param status
     * @param id
     */

   @Update("update employee set status=#{status} where id=#{id}")
    void EnableOrDisableEmployeeAccounts(Integer status, long id);
    @AutoFill(value = OperationType.UPDATE)
   @Update("update employee set id_number=#{idNumber},name=#{name},phone=#{phone},sex=#{sex},username=#{username},update_time=#{updateTime},update_user=#{updateUser} where id=#{id}")
    void Update(Employee employee);
   @Select("select * from employee where id=#{id}")
    Employee selecById(long id);
}
