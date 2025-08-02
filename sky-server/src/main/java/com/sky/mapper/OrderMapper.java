package com.sky.mapper;

import com.github.pagehelper.Page;
import com.sky.dto.GoodsSalesDTO;
import com.sky.dto.OrdersPageQueryDTO;
import com.sky.entity.DateAmount;
import com.sky.entity.OrderCount;
import com.sky.entity.OrderDetail;
import com.sky.entity.Orders;
import com.sky.vo.SalesTop10ReportVO;
import org.apache.ibatis.annotations.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Mapper
public interface OrderMapper {

    /**
     * 插入订单数据
     * @param order
     */
    @Options(useGeneratedKeys = true, keyProperty = "id") // 关键配置  返回自增的主键至传入的参数
   @Insert("insert into orders(number, status, user_id, address_book_id, order_time, checkout_time, pay_method, pay_status, amount, remark, phone, address, user_name, consignee, cancel_reason, rejection_reason, cancel_time, estimated_delivery_time, delivery_status, delivery_time, pack_amount, tableware_number, tableware_status) values " +
           "(#{number},#{status},#{userId},#{addressBookId},#{orderTime},#{checkoutTime},#{payMethod},#{payStatus},#{amount},#{remark},#{phone},#{address},#{userName},#{consignee},#{cancelReason},#{rejectionReason},#{cancelTime},#{estimatedDeliveryTime},#{deliveryStatus},#{deliveryTime},#{packAmount},#{tablewareNumber},#{tablewareStatus})" )
    void insert(Orders order);
    /**
     * 根据订单号查询订单
     * @param orderNumber
     */
    @Select("select * from orders where number = #{orderNumber}")
    Orders getByNumber(String orderNumber);

    /**
     * 修改订单信息
     * @param orders
     */
    void update(Orders orders);

    @Select("select count(*) from orders")
    Integer count();

    Page<Orders> PageQuery(OrdersPageQueryDTO ordersPageQueryDTO);

    @Select("select * from order_detail where order_id=#{id}")
    List<OrderDetail> orderDeatilQuery(int id);

    @Select("select * from orders where id=#{id}")
    Orders getById(Long id);

    @Delete("delete from orders where id=#{id}")
    void detele(Long id);

    @Select("select count(*) from orders where status=#{status}")
    Integer countByStatus(Integer status);

    @Select("select * from orders where status=#{status} and order_time<#{orderTime}")
    List<Orders> getByStatusAndOrderTime(Integer status,LocalDateTime orderTime);

   void updateBatch(@Param("ids") List<Long> ids, Orders orders);

   List<DateAmount> TurnoverStatistics(Map map);

    List<OrderCount> ordersAStatistics(Map map);

    Integer countByStatusAndTime(Map map);

    List<GoodsSalesDTO> top10(Map map);

   Integer countByMap(Map map);

    Double sumByMap(Map map);
}
