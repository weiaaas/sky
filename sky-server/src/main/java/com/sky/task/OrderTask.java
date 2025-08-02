package com.sky.task;

import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.weaver.ast.Or;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 定时处理订单状态
 */
@Component
@Slf4j
public class OrderTask {
    @Autowired
    private OrderMapper orderMapper;
    @Scheduled(cron = "0 0/1 * * * ?")
    @Transactional
    public void proccessTimeoutOrder(){
        log.info("处理超时订单:{}",LocalDateTime.now());
        LocalDateTime time=LocalDateTime.now().plusMinutes(-15);
        //查询状态为待付款状态的订单 且下单时间超过15分钟
        List<Orders> ordersList=orderMapper.getByStatusAndOrderTime(Orders.PENDING_PAYMENT,time);
        //查询出需要修改的订单的id集合
        if(ordersList!=null&&!ordersList.isEmpty()){
            List<Long> ids=ordersList.stream()
                    .map(Orders::getId)
                    .collect(Collectors.toList());
            Orders orders= Orders.builder()
                            .cancelTime(LocalDateTime.now())
                            .cancelReason("订单超时，自动取消")
                            .status(Orders.CANCELLED)
                            .build();
            orderMapper.updateBatch(ids,orders);
        }

    }
    /**
     * 处理派送中的订单
     */
    @Transactional
    @Scheduled(cron = "0 0 1 * * ?")  //凌晨一点
    public void proccessDeliveryOrder(){
        log.info("定时派送中订单:{}", LocalDateTime.now());
        //查询状态为派送中的订单
        LocalDateTime time =LocalDateTime.now().plusMinutes(-60);
        List<Orders> ordersList = orderMapper.getByStatusAndOrderTime(Orders.DELIVERY_IN_PROGRESS, time);

        if(ordersList!=null&&!ordersList.isEmpty()){
            List<Long> ids=ordersList.stream()
                    .map(Orders::getId)
                    .collect(Collectors.toList());
            Orders orders=new Orders();
            orders.setStatus(Orders.COMPLETED);
            //不需要取消原因跟取消时间
            orderMapper.updateBatch(ids,orders);
        }

    }
}
