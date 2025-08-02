package com.sky.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.WebSocket.WebSocketServer;
import com.sky.constant.MessageConstant;
import com.sky.context.BaseContext;
import com.sky.dto.*;
import com.sky.entity.*;
import com.sky.exception.AddressBookBusinessException;
import com.sky.exception.OrderBusinessException;
import com.sky.exception.ShoppingCartBusinessException;
import com.sky.mapper.*;
import com.sky.result.PageResult;
import com.sky.service.OrderService;
import com.sky.utils.HttpClientUtil;
import com.sky.utils.WeChatPayUtil;
import com.sky.vo.OrderPaymentVO;
import com.sky.vo.OrderStatisticsVO;
import com.sky.vo.OrderSubmitVO;
import com.sky.vo.OrderVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
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
    @Autowired
    private WebSocketServer webSocketServer;

    @Value("${sky.shop.address}")
    private String ShopAddress;
    @Value("${sky.baidu.ak}")
    private String ak;


    /**
     * 用户提交订单
     *
     * @param ordersSubmitDTO
     * @return
     */
    @Transactional
    @Override
    public OrderSubmitVO submit(OrdersSubmitDTO ordersSubmitDTO) {
        //异常处理 收货地址为空时
        AddressBook addressBook = addressBookMapper.getById(ordersSubmitDTO.getAddressBookId());
        if (addressBook == null) {
            throw new AddressBookBusinessException(MessageConstant.ADDRESS_BOOK_IS_NULL);
        }
        //检查是否超出配送距离
        CheckOutRange(addressBook.getCityName()+addressBook.getDistrictName()+addressBook.getDetail());

        //异常处理  购物车为空时
        Long userId = BaseContext.getCurrentId();
        ShoppingCart shoppingCart = new ShoppingCart();
        shoppingCart.setUserId(userId);
        List<ShoppingCart> ShoppingCartList = shoppingCartMapper.list(shoppingCart);
        if (ShoppingCartList == null || ShoppingCartList.size() == 0) {
            throw new ShoppingCartBusinessException(MessageConstant.SHOPPING_CART_IS_NULL);
        }


        //构造订单数据
        Orders order = new Orders();
        BeanUtils.copyProperties(ordersSubmitDTO, order);
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
        List<OrderDetail> orderDetailList = new ArrayList<>();
        for (ShoppingCart cart : ShoppingCartList) {
            OrderDetail orderDetail = new OrderDetail();
            BeanUtils.copyProperties(cart, orderDetail);
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
        //通过webSocket推送消息到商户
        Map map=new HashMap();
        map.put("type",1); //1为来单提醒，2为用户催单
        map.put("orderId",ordersDB.getId());
        map.put("content","订单号"+outTradeNo);
        String json = JSON.toJSONString(map);
        webSocketServer.sendToAllClient(json);
    }

    /**
     * 查询历史订单
     *
     * @param page
     * @param pageSize
     * @param status
     * @return
     */
    @Override
    public PageResult historyOrders(Integer page, Integer pageSize, Integer status) {
        //使用mybatis分页查询插件
        PageHelper.startPage(page, pageSize);

        OrdersPageQueryDTO ordersPageQueryDTO = new OrdersPageQueryDTO();
        ordersPageQueryDTO.setUserId(BaseContext.getCurrentId());
        ordersPageQueryDTO.setStatus(status);

        Page<Orders> pageResult = orderMapper.PageQuery(ordersPageQueryDTO);

        List<Long> ids=pageResult.getResult().stream()
                .map(Orders::getId)
                .collect(Collectors.toList());
        Map<Long,List<OrderDetail>> OrderDetailMap=orderDetailMapper.getByIds(ids).stream()
                .collect(Collectors.groupingBy(OrderDetail::getOrderId));
//        for (Orders orders : pageResult) {
//            Long orderID = orders.getId();
//            List<OrderDetail> orderDetail = orderDetailMapper.getByID(orderID);
//            OrderVO orderVO = new OrderVO();
//            BeanUtils.copyProperties(orders, orderVO);
//            orderVO.setOrderDetailList(orderDetail);
//            list.add(orderVO);
//        }
        //结果封装
        List<OrderVO> list = pageResult.getResult().stream()
                .map(orders->{
                    OrderVO orderVO=new OrderVO();
                    BeanUtils.copyProperties(orders,orderVO);
                    List<OrderDetail> orderDetailList=OrderDetailMap.getOrDefault(orders.getId(),Collections.emptyList());
                    orderVO.setOrderDetailList(orderDetailList);
                    return orderVO;
                })
                .collect(Collectors.toList());
        return new PageResult(pageResult.getTotal(), list);
    }

    /**
     * 订单详细
     *
     * @param id
     * @return
     */
    @Override
    public OrderVO detail(Long id) {
        Orders orders = orderMapper.getById(id);
        OrderVO orderVO = new OrderVO();
        BeanUtils.copyProperties(orders, orderVO);
        List<OrderDetail> list = orderDetailMapper.getByID(id);
        orderVO.setOrderDetailList(list);
        return orderVO;
    }

    /**
     * 再来一单
     *
     * @param id
     */
    @Override
    public void repetition(Long id) {
        List<OrderDetail> orderDetails = orderDetailMapper.getByID(id);
        Long userId = BaseContext.getCurrentId();

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
        Orders orders = orderMapper.getById(id);
        //订单是否存在
        if (orders == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        //订单状态 1待付款 2待接单 3已接单 4派送中 5已完成 6已取消
        //如果订单处于3 4 5 6四种情况无法退款
        if (orders.getStatus() > 2) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }
        Orders cancelOrders = new Orders();
        cancelOrders.setId(orders.getId());
        //如果在等待接单时取消订单需要退款
        if (orders.getStatus().equals(OrderVO.TO_BE_CONFIRMED)) {
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
     *
     * @param ordersPageQueryDTO
     * @return
     */
    @Override
    public PageResult conditionSearch(OrdersPageQueryDTO ordersPageQueryDTO) {

        PageHelper.startPage(ordersPageQueryDTO.getPage(), ordersPageQueryDTO.getPageSize());
        Page<Orders> pages = orderMapper.PageQuery(ordersPageQueryDTO);

        List<Long> ids=pages.getResult().stream()
                .map(Orders::getId)
                .collect(Collectors.toList());
        if(ids==null||ids.isEmpty()){
            return new PageResult(pages.getTotal(), Collections.emptyList());
        }
        Map<Long,List<OrderDetail>> OrderDetailtMap=orderDetailMapper.getByIds(ids).stream()
                .collect(Collectors.groupingBy(OrderDetail::getOrderId));
        List<OrderVO> orderVOList=pages.getResult().stream()
                .map(x->{
                    OrderVO orderVO=new OrderVO();
                    BeanUtils.copyProperties(x,orderVO);
                    //从Map获取订单详细
                    List<OrderDetail> orderDetailList=OrderDetailtMap.getOrDefault(x.getId(),Collections.emptyList());
                    //构造菜品 name*nummber;
                    String orderDishs=orderDetailList.stream()
                            .map(orderDetail -> {
                                String str=orderDetail.getName()+"*"+orderDetail.getNumber()+";";
                                return str;
                            }).collect(Collectors.joining());//拼接所有字符串
                    //设置菜品信息到orderVo
                    orderVO.setOrderDishes(orderDishs);
                    return orderVO;
                })
                .collect(Collectors.toList()); //收集成List<OrderVo>
        return new PageResult(pages.getTotal(),orderVOList);
    }

    @Override
    public OrderStatisticsVO statistics() {
        OrderStatisticsVO orderStatisticsVO=new OrderStatisticsVO();
        Integer confirmed= orderMapper.countByStatus(Orders.CONFIRMED);
        Integer toBeConfirmed=orderMapper.countByStatus(Orders.TO_BE_CONFIRMED);
        Integer deliveryInProgress=orderMapper.countByStatus(Orders.DELIVERY_IN_PROGRESS);
        orderStatisticsVO.setConfirmed(confirmed);
        orderStatisticsVO.setToBeConfirmed(toBeConfirmed);
        orderStatisticsVO.setDeliveryInProgress(deliveryInProgress);
        return orderStatisticsVO;
    }

    @Override
    public void confirm(OrdersConfirmDTO ordersConfirmDTO) {
        Orders orders = orderMapper.getById(ordersConfirmDTO.getId());
        orders.setStatus(Orders.CONFIRMED);
        orderMapper.update(orders);
    }

    @Override
    public void reject(OrdersRejectionDTO ordersRejectionDTO) throws Exception {
        Orders orders=orderMapper.getById(ordersRejectionDTO.getId());
        if(orders==null||!orders.getStatus().equals(Orders.TO_BE_CONFIRMED)){
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }
        if(orders.getPayStatus()==Orders.PAID){
            String refund =weChatPayUtil.refund(
                    orders.getNumber(),//商户订单号
                    orders.getNumber(),//商户退款号
                    new BigDecimal(0.01),//退款金额
                    new BigDecimal(0.01) //原订单金额
            );
            log.info("进行退款:{}",refund);
        }
        //更新 取消时间 取消原因 订单状态
        orders.setStatus(Orders.CANCELLED);
        orders.setRejectionReason(ordersRejectionDTO.getRejectionReason());
        orders.setCancelTime(LocalDateTime.now());
        orderMapper.update(orders);
    }

    @Override
    public void S_cancel(OrdersCancelDTO ordersCancelDTO) throws Exception {
        Orders orders=orderMapper.getById(ordersCancelDTO.getId());
        if(orders==null){
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }
        if(orders.getPayStatus()==Orders.PAID){
            String refund=weChatPayUtil.refund(
                    orders.getNumber(),//商户订单号
                    orders.getNumber(),//商户退款号
                    new BigDecimal(0.01),//退款金额
                    new BigDecimal(0.01) //原订单金额
            );
            log.info("进行退款:{}",refund);
        }
        //更新 取消时间 取消原因 订单状态
        orders.setCancelReason(ordersCancelDTO.getCancelReason());
        orders.setCancelTime(LocalDateTime.now());
        orders.setStatus(Orders.CANCELLED);
        orderMapper.update(orders);
    }

    @Override
    public void delivery(Long id) {
        Orders orders=orderMapper.getById(id);
        if(orders==null||!orders.getStatus().equals(Orders.CONFIRMED)){
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }
        orders.setStatus(Orders.DELIVERY_IN_PROGRESS);
        orderMapper.update(orders);
    }

    @Override
    public void complete(Long id) {
        Orders orders=orderMapper.getById(id);
        if(orders==null||!orders.getStatus().equals(Orders.DELIVERY_IN_PROGRESS)){
            throw  new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }
        orders.setStatus(Orders.COMPLETED);
        orderMapper.update(orders);
    }

    @Override
    public void remider(Long id) {
        Orders orders = orderMapper.getById(id);
        if(orders==null){
            throw  new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);

        }
        Map map=new HashMap();
        map.put("type",2);//客户催单
        map.put("orderID",id);
        map.put("content","订单号"+orders.getNumber());
        String jsonString = JSON.toJSONString(map);
        //使用websocket发送催单信息
        webSocketServer.sendToAllClient(jsonString);

    }

    private void CheckOutRange(String address){
        Map map=new HashMap();
        map.put("address",ShopAddress);
        map.put("output","json");
        map.put("ak",ak);

        //获取商铺经纬度坐标
        String shopCoordinate= HttpClientUtil.doGet("https://api.map.baidu.com/geocoding/v3",map);
        JSONObject jsonObject = JSONObject.parseObject(shopCoordinate);
        if(!jsonObject.getString("status").equals("0")){
            throw new OrderBusinessException("店铺地址解析失败");
        }
        //解析数据
        JSONObject location=jsonObject.getJSONObject("result").getJSONObject("location");
        String lat=location.getString("lat");
        String lng=location.getString("lng");
        //店铺经纬度坐标
        String shoplnglat=lat+","+lng;

        map.put("address",address);
        //获取用户收货地址的经纬度  坐标
        String userCoordinate=HttpClientUtil.doGet("https://api.map.baidu.com/geocoding/v3",map);
        jsonObject=JSONObject.parseObject(userCoordinate);
        if(!jsonObject.getString("status").equals("0")){
            throw new OrderBusinessException("店铺地址解析失败");
        }
        //解析数据
        location=jsonObject.getJSONObject("result").getJSONObject("location");
        lat=location.getString("lat");
        lng=location.getString("lng");
        //获取用户经纬度坐标
        String userlnglat=lat+","+lng;

        map.put("origin",shoplnglat);
        map.put("destination",userlnglat);
        map.put("step_info","0");

        //路线规划
        String json=HttpClientUtil.doGet("https://api.map.baidu.com/directionlite/v1/driving",map);
        jsonObject=JSONObject.parseObject(json);
        if(!jsonObject.getString("status").equals("0")){
            throw new OrderBusinessException("配送路线规划失败");
        }

        //数据解析
        JSONObject result=jsonObject.getJSONObject("result");
        JSONArray jsonArray= (JSONArray) result.get("routes");
        Integer distance= (Integer) ((JSONObject)jsonArray.get(0)).get("distance");
        if(distance>5000){
            //配送距离超过5000米
            throw new OrderBusinessException("超出配送范围");
        }
    }
}