package com.sky.mapper;

import com.sky.entity.OrderDetail;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface OrderDetailMapper {
    List<OrderDetail> getByID(Long orderID);

    void insertBatch(List<OrderDetail> orderDetailList);

    @Delete("delete from order_detail where id=#{id}")
    void detele(Long id);
}
