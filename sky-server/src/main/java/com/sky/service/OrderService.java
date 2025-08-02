package com.sky.service;

import com.sky.dto.*;
import com.sky.result.PageResult;
import com.sky.vo.OrderPaymentVO;
import com.sky.vo.OrderStatisticsVO;
import com.sky.vo.OrderSubmitVO;
import com.sky.vo.OrderVO;

public interface OrderService {
    OrderSubmitVO submit(OrdersSubmitDTO ordersSubmitDTO);

    OrderPaymentVO payment(OrdersPaymentDTO ordersPaymentDTO) throws Exception;

    /**
     * 支付成功，修改订单状态
     * @param outTradeNo
     */
    void paySuccess(String outTradeNo);

    PageResult historyOrders(Integer page,Integer pageSize,Integer status);


    OrderVO detail(Long id);

    void repetition(Long id);

    void cancel(Long id) throws Exception;

    PageResult conditionSearch(OrdersPageQueryDTO ordersPageQueryDTO);

    OrderStatisticsVO statistics();

    void confirm(OrdersConfirmDTO ordersConfirmDTO);

    void reject(OrdersRejectionDTO ordersRejectionDTO) throws Exception;

    void S_cancel(OrdersCancelDTO ordersCancelDTO) throws Exception;

    void delivery(Long id);

    void complete(Long id);

    void remider(Long id);
}
