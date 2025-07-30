package com.sky.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.context.BaseContext;
import com.sky.dto.OrdersDTO;
import com.sky.dto.OrdersPageQueryDTO;
import com.sky.dto.OrdersPaymentDTO;
import com.sky.dto.OrdersSubmitDTO;
import com.sky.entity.*;
import com.sky.exception.AddressBookBusinessException;
import com.sky.exception.OrderBusinessException;
import com.sky.exception.ShoppingCartBusinessException;
import com.sky.mapper.*;
import com.sky.result.PageResult;
import com.sky.service.OrderService;
import com.sky.utils.WeChatPayUtil;
import com.sky.vo.OrderPaymentVO;
import com.sky.vo.OrderSubmitVO;
import com.sky.vo.OrderVO;
import org.aspectj.weaver.ast.Or;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class OrderImpl implements OrderService {
    @Autowired
    private ShoppingCartMapper shoppingCartMapper;
    @Autowired
    private AddressBookMapper addressBookMapper;
    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private OrderDetailMapper orderDetailMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private WeChatPayUtil weChatPayUtil;

    /**
     * 用户提交订单
     * @param ordersSubmitDTO
     * @return
     */
    @Transactional
    @Override
    public OrderSubmitVO submit(OrdersSubmitDTO ordersSubmitDTO) {
        //异常处理 收货地址为空时
        AddressBook addressBook = addressBookMapper.getById(ordersSubmitDTO.getAddressBookId());
        if(addressBook==null){
            throw new AddressBookBusinessException(MessageConstant.ADDRESS_BOOK_IS_NULL);
        }

        //异常处理  购物车为空时
        Long userId = BaseContext.getCurrentId();
        ShoppingCart shoppingCart=new ShoppingCart();
        shoppingCart.setUserId(userId);
        List<ShoppingCart> ShoppingCartList = shoppingCartMapper.list(shoppingCart);
        if(ShoppingCartList==null||ShoppingCartList.size()==0){
            throw new ShoppingCartBusinessException(MessageConstant.SHOPPING_CART_IS_NULL);
        }

        //构造订单数据
        Orders order=new Orders();
        BeanUtils.copyProperties(ordersSubmitDTO,order);
        order.setPhone(addressBook.getPhone());
        order.setAddress(addressBook.getDetail());
        order.setConsignee(addressBook.getConsignee());
        //System.currentTimeMillis() 的作用
        //        返回 当前时间与 1970年1月1日 UTC 时间（纪元时间）的毫秒差值（即时间戳）。
        //使用时间戳作为订单号
        order.setNumber(String.valueOf(System.currentTimeMillis()));
        order.setUserId(userId);
        order.setStatus(Orders.PENDING_PAYMENT);
        order.setPayStatus(Orders.UN_PAID);
        order.setOrderTime(LocalDateTime.now());

        //向订单表插入数据
        orderMapper.insert(order);
        //订单明细数据
        List<OrderDetail> orderDetailList=new ArrayList<>();
        for(ShoppingCart cart:ShoppingCartList){
            OrderDetail orderDetail=new OrderDetail();
            BeanUtils.copyProperties(cart,orderDetail);
            orderDetail.setOrderId(order.getId());

            orderDetailList.add(orderDetail);
        }
        //插入多条数据到 订单细节表
        orderDetailMapper.insertBatch(orderDetailList);
        //删除购物车
        shoppingCartMapper.clean(userId);

        //封装返回结果
        OrderSubmitVO orderSubmitVO = new OrderSubmitVO();
        orderSubmitVO.setId(order.getId());
        orderSubmitVO.setOrderNumber(order.getNumber());
        orderSubmitVO.setOrderAmount(order.getAmount());
        orderSubmitVO.setOrderTime(order.getOrderTime());
        return orderSubmitVO;
    }

    /**
     * 订单支付
     *
     * @param ordersPaymentDTO
     * @return
     */
    public OrderPaymentVO payment(OrdersPaymentDTO ordersPaymentDTO) throws Exception {
        // 当前登录用户id
        Long userId = BaseContext.getCurrentId();
        User user = userMapper.getById(userId);

        //调用微信支付接口，生成预支付交易单
        JSONObject jsonObject = weChatPayUtil.pay(
                ordersPaymentDTO.getOrderNumber(), //商户订单号
                new BigDecimal(0.01), //支付金额，单位 元
                "苍穹外卖订单", //商品描述
                user.getOpenid() //微信用户的openid
        );

        if (jsonObject.getString("code") != null && jsonObject.getString("code").equals("ORDERPAID")) {
            throw new OrderBusinessException("该订单已支付");
        }

        OrderPaymentVO vo = jsonObject.toJavaObject(OrderPaymentVO.class);
        vo.setPackageStr(jsonObject.getString("package"));

        return vo;
    }

    /**
     * 支付成功，修改订单状态
     *
     * @param outTradeNo
     */
    public void paySuccess(String outTradeNo) {

        // 根据订单号查询订单
        Orders ordersDB = orderMapper.getByNumber(outTradeNo);

        // 根据订单id更新订单的状态、支付方式、支付状态、结账时间
        Orders orders = Orders.builder()
                .id(ordersDB.getId())
                .status(Orders.TO_BE_CONFIRMED)
                .payStatus(Orders.PAID)
                .checkoutTime(LocalDateTime.now())
                .build();

        orderMapper.update(orders);
    }

    /**
     * 查询历史订单
     * @param page
     * @param pageSize
     * @param status
     * @return
     */
    @Override
    public PageResult historyOrders(Integer page,Integer pageSize,Integer status) {
        //使用mybatis分页查询插件
        PageHelper.startPage(page,pageSize);

        OrdersPageQueryDTO ordersPageQueryDTO=new OrdersPageQueryDTO();
        ordersPageQueryDTO.setUserId(BaseContext.getCurrentId());
        ordersPageQueryDTO.setStatus(status);

        Page<Orders> pageResult=orderMapper.PageQuery(ordersPageQueryDTO);
        //结果封装
        List<OrderVO> list=new ArrayList<>();
        //构建返回数据
        for (Orders orders:pageResult){
            Long orderID=orders.getId();
            List<OrderDetail> orderDetail= orderDetailMapper.getByID(orderID);
            OrderVO orderVO=new OrderVO();
            BeanUtils.copyProperties(orders,orderVO);
            orderVO.setOrderDetailList(orderDetail);
            list.add(orderVO);
        }
        return new PageResult(pageResult.getTotal(),list);
    }

    /**
     * 订单详细
     * @param id
     * @return
     */
    @Override
    public OrderVO detail(Long id) {
        Orders orders=orderMapper.getById(id);
        OrderVO orderVO=new OrderVO();
        BeanUtils.copyProperties(orders,orderVO);
        List<OrderDetail> list = orderDetailMapper.getByID(id);
        orderVO.setOrderDetailList(list);
        return orderVO;
    }

    /**
     * 再来一单
     * @param id
     */
    @Override
    public void repetition(Long id) {
        List<OrderDetail> orderDetails = orderDetailMapper.getByID(id);
        Long userId=BaseContext.getCurrentId();

        // 将订单详情对象转换为购物车对象
        List<ShoppingCart> shoppingCartList = orderDetails.stream().map(x -> {
            ShoppingCart shoppingCart = new ShoppingCart();
            // 将原订单详情里面的菜品信息重新复制到购物车对象中
            BeanUtils.copyProperties(x, shoppingCart, "id");
            shoppingCart.setUserId(userId);
            shoppingCart.setCreateTime(LocalDateTime.now());
            return shoppingCart;
        }).collect(Collectors.toList());
        shoppingCartMapper.insertBatch(shoppingCartList);
    }

    @Override
    public void cancel(Long id) throws Exception {
        //处理异常
        Orders orders=orderMapper.getById(id);
        //订单是否存在
        if(orders==null){
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        //订单状态 1待付款 2待接单 3已接单 4派送中 5已完成 6已取消
        //如果订单处于3 4 5 6四种情况无法退款
        if(orders.getStatus()>2){
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }
        Orders cancelOrders=new Orders();
        cancelOrders.setId(orders.getId());
        //如果在等待接单时取消订单需要退款
        if(orders.getStatus().equals(OrderVO.TO_BE_CONFIRMED)){
            weChatPayUtil.refund(
                    orders.getNumber(),//商户订单号
                    orders.getNumber(),//商户退款号
                    new BigDecimal(0.01),//退款金额
                    new BigDecimal(0.01) //原订单金额
            );

            //订单状态设置为退款
            cancelOrders.setStatus(Orders.REFUND);
        }
        //设置为已取消
        cancelOrders.setStatus(Orders.CANCELLED);
        cancelOrders.setCancelTime(LocalDateTime.now());
        cancelOrders.setCancelReason("用户已取消");
        orderMapper.update(cancelOrders);
    }

    /**
     * 订单搜索
     * @param ordersPageQueryDTO
     * @return
     */
    @Override
    public PageResult conditionSearch(OrdersPageQueryDTO ordersPageQueryDTO) {
        PageHelper.startPage(ordersPageQueryDTO.getPage(), ordersPageQueryDTO.getPageSize());
        Page<Orders> pages = orderMapper.PageQuery(ordersPageQueryDTO);

        if (pages.isEmpty()) {
            return new PageResult(0, Collections.emptyList());
        }

        // 获取订单ID列表
        List<Long> orderIds = pages.getResult().stream()
                .map(Orders::getId)
                .collect(Collectors.toList());

        // 批量查询所有订单详情
        Map<Long, List<OrderDetail>> orderDetailMap = orderDetailMapper.getByOrderIds(orderIds).stream()
                .collect(Collectors.groupingBy(OrderDetail::getOrderId));

        // 转换订单数据
        List<OrderVO> orderVOList = pages.getResult().stream()
                .map(orders -> {
                    OrderVO orderVO = new OrderVO();
                    BeanUtils.copyProperties(orders, orderVO);

                    // 从Map中获取订单详情，避免重复查询
                    List<OrderDetail> orderDetails = orderDetailMap.getOrDefault(orders.getId(), Collections.emptyList());

                    String orderDishes = orderDetails.stream()
                            .map(OrderDetail::getName)
                            .collect(Collectors.joining(" "));

                    orderVO.setOrderDishes(orderDishes);
                    return orderVO;
                })
                .collect(Collectors.toList());

        return new PageResult(pages.getTotal(), orderVOList);
    }

}
