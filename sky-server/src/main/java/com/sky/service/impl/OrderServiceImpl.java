package com.sky.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.context.BaseContext;
import com.sky.dto.*;
import com.sky.entity.*;
import com.sky.exception.AddressBookBusinessException;
import com.sky.exception.OrderBusinessException;
import com.sky.mapper.*;
import com.sky.properties.ShopProperties;
import com.sky.result.PageResult;
import com.sky.service.OrderService;
import com.sky.utils.BaiduMapUtil;
import com.sky.vo.OrderPaymentVO;
import com.sky.vo.OrderStatisticsVO;
import com.sky.vo.OrderSubmitVO;
import com.sky.vo.OrderVO;
import com.sky.websocket.WebSocketServer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
public class OrderServiceImpl implements OrderService {

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private OrderDetailMapper orderDetailMapper;

    @Autowired
    private AddressBookMapper addressBookMapper;

    @Autowired
    private ShoppingCartMapper shoppingCartMapper;

    @Autowired
    private DishMapper dishMapper;

    @Autowired
    private ShopProperties shopProperties;  // 获取商家地址和AK

    @Autowired
    private BaiduMapUtil baiduMapUtil;      // 调用百度地图

    @Autowired
    private WebSocketServer webSocketServer;

    @Autowired
    private UserMapper userMapper;
    // 常量：最大配送距离5000米（5公里）
    private static final int MAX_DELIVERY_DISTANCE = 5000;

    /**
     * 用户下单
     * @param ordersSubmitDTO
     * @return
     */
    @Transactional
    @Override
    public OrderSubmitVO submit(OrdersSubmitDTO ordersSubmitDTO) {

        // ========== 第1步：校验地址簿是否存在 ==========
        AddressBook addressBook = addressBookMapper.getById(ordersSubmitDTO.getAddressBookId());
        if(addressBook == null){
            throw new AddressBookBusinessException(MessageConstant.ADDRESS_BOOK_IS_NULL);
        }

        // ========== 第2步：校验购物车是否为空 ==========
        ShoppingCart shoppingCart = new ShoppingCart();
        shoppingCart.setUserId(BaseContext.getCurrentId());
        List<ShoppingCart> list = shoppingCartMapper.list(shoppingCart);
        if(list.isEmpty()){
            throw new AddressBookBusinessException(MessageConstant.SHOPPING_CART_IS_NULL);
        }

        // ========== 第3步：⭐ 配送范围校验（新增的核心逻辑）==========

        // ① 获取用户的详细地址（拼接完整地址）
        String userAddress = buildFullAddress(addressBook);

        // ② 获取商家地址（从配置文件）
        String shopAddress = shopProperties.getAddress();

        // ③ 获取百度地图AK（从配置文件）
        String baiduMapAk = shopProperties.getBaiduMapAk();

        // ④ 判断是否在配送范围内
        if (baiduMapAk != null && !baiduMapAk.isEmpty()) {
            // 调用工具类，传入用户地址、商家地址、AK、最大距离
            boolean inRange = baiduMapUtil.isInDeliveryRange(
                    userAddress,      // 用户地址
                    shopAddress,      // 商家地址
                    baiduMapAk,       // 百度地图密钥
                    MAX_DELIVERY_DISTANCE  // 5000米
            );

            // ⑤ 如果超出范围，抛出异常，阻止下单
            if (!inRange) {
                throw new OrderBusinessException("超出配送范围，无法下单");
            }
        } else {
            // 如果没有配置AK，记录警告但不阻止下单（开发环境友好）
            log.warn("未配置百度地图AK，跳过配送范围校验");
        }
        // ========== 第4步：创建订单（原有逻辑）==========
        Orders orders = new Orders();
        BeanUtils.copyProperties(ordersSubmitDTO, orders);
        orders.setOrderTime(LocalDateTime.now());
        orders.setPayStatus(Orders.UN_PAID);
        orders.setStatus(Orders.PENDING_PAYMENT);
        orders.setNumber(String.valueOf(System.currentTimeMillis()));
        orders.setPhone(addressBook.getPhone());
        orders.setConsignee(addressBook.getConsignee());
        orders.setUserId(BaseContext.getCurrentId());

        // 插入订单表
        orderMapper.insert(orders);

        // ========== 第5步：创建订单明细（原有逻辑）==========
        List<OrderDetail> orderDetailList = new ArrayList<>();
        for (ShoppingCart cart : list) {
            OrderDetail orderDetail = new OrderDetail();
            BeanUtils.copyProperties(cart, orderDetail);
            orderDetail.setOrderId(orders.getId());
            orderDetailList.add(orderDetail);
        }
        orderDetailMapper.insertBatch(orderDetailList);

        // ========== 第6步：清空购物车（原有逻辑）==========
        shoppingCartMapper.delete(shoppingCart);

        // ========== 第7步：返回订单信息 ==========
        return OrderSubmitVO.builder()
                .id(orders.getId())
                .orderNumber(orders.getNumber())
                .orderAmount(ordersSubmitDTO.getAmount())
                .orderTime(orders.getOrderTime())
                .build();
    }

    @Override
    @Transactional
    public OrderPaymentVO payment(OrdersPaymentDTO ordersPaymentDTO) {
        // 当前登录用户id
        Long userId = BaseContext.getCurrentId();
        User user = userMapper.getById(userId);

        //调用微信支付接口，生成预支付交易单
            /*JSONObject jsonObject = weChatPayUtil.pay(
                    ordersPaymentDTO.getOrderNumber(), //商户订单号
                    new BigDecimal(0.01), //支付金额，单位 元
                    "苍穹外卖订单", //商品描述
                    user.getOpenid() //微信用户的openid
            );
            if (jsonObject.getString("code") != null && jsonObject.getString("code").equals("ORDERPAID")) {
                throw new OrderBusinessException("该订单已支付");
            }*/

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("code","ORDERPAID");
        OrderPaymentVO vo = jsonObject.toJavaObject(OrderPaymentVO.class);
        vo.setPackageStr(jsonObject.getString("package"));

        // 为替代微信支付成功后的数据库状态更新，多定义一个方法进行修改
        Integer OrderPaidStatus = Orders.PAID; // 支付状态，已支付
        Integer OrderStatus = Orders.TO_BE_CONFIRMED; // 订单状态，待接单

        // 发现没有将支付时间 check_out属性赋值，所以在这里更新
        LocalDateTime check_out_time = LocalDateTime.now();

        // 获取订单号码
        String orderNumber = ordersPaymentDTO.getOrderNumber();

        Orders ordersDB = orderMapper.getByNumber(orderNumber);
        log.info("调用updateStatus，用于替换微信支付更新数据库状态的问题");
        orderMapper.updateStatus(OrderStatus, OrderPaidStatus, check_out_time, orderNumber);

        //通过WebSocket向客户端浏览器推送消息 type orderId content
        Map map = new HashMap();
        map.put("type", 1);  //1表示来单提醒 2表示客户催单
        map.put("orderId", ordersDB.getId());
        map.put("content", "订单号：" + orderNumber);

        String json = JSON.toJSONString(map);
        webSocketServer.sendToAllClient(json);

        return vo;
    }

    @Override
    public PageResult list(OrdersPageQueryDTO ordersPageQueryDTO) {
        Long currentId = BaseContext.getCurrentId();
        log.info("======= 订单列表查询开始 =======");
        log.info("当前用户ID: {}", currentId);

        ordersPageQueryDTO.setUserId(currentId);
        PageHelper.startPage(ordersPageQueryDTO.getPage(), ordersPageQueryDTO.getPageSize());
        Page<Orders> page = orderMapper.list(ordersPageQueryDTO);

        List<Orders> records = new ArrayList<>();
        for (Orders order : page.getResult()) {
            order.setOrderDetailList(orderDetailMapper.listByOrderId(order.getId()));
            records.add(order);
        }

        PageResult pageResult = new PageResult(page.getTotal(), records);

        log.info("查询结果: total={}, records.size={}", pageResult.getTotal(), pageResult.getRecords().size());
        log.info("======= 订单列表查询结束 =======");

        return pageResult;
    }

    @Override
    public OrderVO getOrderDetail(Long id) {
        log.info("订单详情，订单id为：{}", id);
        Orders orders = orderMapper.getById(id);
        OrderVO orderVO = new OrderVO();
        BeanUtils.copyProperties(orders, orderVO);
        List<OrderDetail> orderDetailList = orderDetailMapper.listByOrderId(id);
        orderVO.setOrderDetailList(orderDetailList);
        orderVO.setOrderDishes(getOrderDishesString(orderDetailList));
        return orderVO;
    }

    @Override
    public void cancel(Long id) {
        Orders orders = orderMapper.getById(id);
        if(orders == null){
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        if(!Objects.equals(orders.getStatus(), Orders.PENDING_PAYMENT)
        && !Objects.equals(orders.getStatus(), Orders.TO_BE_CONFIRMED)){
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }
        orders.setStatus(Orders.CANCELLED);
        orders.setCancelReason("用户取消");
        orders.setCancelTime(LocalDateTime.now());
        orderMapper.update(orders);
    }

    @Override
    @Transactional
    public void repetition(Long id) {
        log.info("再来一单，订单id: {}", id);

        // 1. 查询原订单
        Orders oldOrder = orderMapper.getById(id);
        if(oldOrder == null){
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }

        // 2. 查询原订单的明细
        List<OrderDetail> oldOrderDetails = orderDetailMapper.listByOrderId(id);
        if(oldOrderDetails == null || oldOrderDetails.isEmpty()){
            throw new OrderBusinessException("订单明细不存在");
        }

        // 3. 创建新订单
        Orders newOrder = new Orders();
        BeanUtils.copyProperties(oldOrder, newOrder);
        newOrder.setId(null); // 清空ID，让数据库自动生成
        newOrder.setNumber(String.valueOf(System.currentTimeMillis())); // 生成新订单号
        newOrder.setStatus(Orders.PENDING_PAYMENT); // 设置为待付款
        newOrder.setPayStatus(Orders.UN_PAID); // 设置为未支付
        newOrder.setOrderTime(LocalDateTime.now()); // 设置下单时间
        newOrder.setCheckoutTime(null);
        newOrder.setCancelTime(null);
        newOrder.setCancelReason(null);
        newOrder.setRejectionReason(null);

        // 4. 插入新订单
        orderMapper.insert(newOrder);

        // 5. 创建新订单明细
        List<OrderDetail> newOrderDetails = new ArrayList<>();
        for (OrderDetail oldDetail : oldOrderDetails) {
            OrderDetail newDetail = new OrderDetail();
            BeanUtils.copyProperties(oldDetail, newDetail);
            newDetail.setId(null); // 清空ID
            newDetail.setOrderId(newOrder.getId()); // 关联新订单ID
            newOrderDetails.add(newDetail);
        }

        // 6. 批量插入新订单明细
        orderDetailMapper.insertBatch(newOrderDetails);

        log.info("再来一单完成，新订单ID: {}", newOrder.getId());
    }

    /**
     * 获取订单菜品信息
     * @param ordersPageQueryDTO 订单明细列表
     * @return 订单菜品信息
     */
    @Override
    public PageResult conditionSearch(OrdersPageQueryDTO ordersPageQueryDTO) {
        PageHelper.startPage(ordersPageQueryDTO.getPage(), ordersPageQueryDTO.getPageSize());
        Page<Orders> orders = orderMapper.list(ordersPageQueryDTO);
        List<Orders> records = new ArrayList<>();
        orders.forEach(order -> {
            List<OrderDetail> orderDetailList = orderDetailMapper.listByOrderId(order.getId());
            order.setOrderDetailList(orderDetailList);
            String orderDishes = getOrderDishesString(orderDetailList);
            order.setOrderDishes(orderDishes);
            records.add(order);
        });
          return   new PageResult(orders.getTotal(), records);
    }

    @Override
    public OrderStatisticsVO statistics() {
        log.info("统计接口");
        OrderStatisticsVO orderStatisticsVO = new OrderStatisticsVO();
        orderStatisticsVO.setToBeConfirmed(0);
        orderStatisticsVO.setConfirmed(0);
        orderStatisticsVO.setDeliveryInProgress(0);
        List<Orders> orders =orderMapper.ordersList();
        orders.forEach(order -> {
            if (order.getStatus() == Orders.TO_BE_CONFIRMED){
                orderStatisticsVO.setToBeConfirmed(orderStatisticsVO.getToBeConfirmed() + 1);
            }else if (order.getStatus() == Orders.CONFIRMED){
                orderStatisticsVO.setConfirmed(orderStatisticsVO.getConfirmed() + 1);
            }else if (order.getStatus() == Orders.DELIVERY_IN_PROGRESS){
                orderStatisticsVO.setDeliveryInProgress(orderStatisticsVO.getDeliveryInProgress() + 1);
            }
        });
        return orderStatisticsVO;
    }

    @Override
    public void confirm(OrdersConfirmDTO ordersConfirmDTO) {
        Orders orders = new Orders();
        orders.setId(ordersConfirmDTO.getId());
        orders.setStatus(Orders.CONFIRMED);
        orderMapper.update(orders);
    }

    /**
     * 订单拒绝
     * @param ordersRejectionDTO
     */
    @Override
    public void rejection(OrdersRejectionDTO ordersRejectionDTO) {
        Orders orders = new Orders();
        orders.setId(ordersRejectionDTO.getId());
        orders.setStatus(Orders.CANCELLED);
        orders.setRejectionReason(ordersRejectionDTO.getRejectionReason());
        orderMapper.update(orders);
    }

    @Override
    public void cancelOrders(OrdersCancelDTO ordersCancelDTO) {
        Orders orders = new Orders();
        orders.setId(ordersCancelDTO.getId());
        orders.setStatus(Orders.CANCELLED);
        orders.setCancelReason(ordersCancelDTO.getCancelReason());
        orderMapper.update(orders);
    }

    @Override
    public void delivery(Long id) {
        Orders orders = new Orders();
        orders.setId(id);
        orders.setStatus(Orders.DELIVERY_IN_PROGRESS);
        log.info("订单开始配送，订单ID: {}", id);
        orderMapper.update(orders);
    }

    @Override
    public void complete(Long id) {
        Orders orders = new Orders();
        orders.setId(id);
        orders.setStatus(Orders.COMPLETED);
        log.info("订单完成，订单ID: {}", id);
        orderMapper.update(orders);
    }

    @Override
    public void paySuccess(String outTradeNo) {
        log.info("支付成功回调处理，订单号: {}", outTradeNo);

        // 根据订单号查询订单
        Orders orders = orderMapper.getByNumber(outTradeNo);
        if (orders == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }

        // 如果订单已经是支付状态，直接返回（避免重复处理）
        if (Objects.equals(orders.getPayStatus(), Orders.PAID)) {
            log.warn("订单已支付，无需重复处理，订单号: {}", outTradeNo);
            return;
        }

        // 更新订单状态为待接单，支付状态为已支付
        orders.setStatus(Orders.TO_BE_CONFIRMED);
        orders.setPayStatus(Orders.PAID);
        orders.setCheckoutTime(LocalDateTime.now());
        orderMapper.update(orders);

        log.info("支付成功处理完成，订单号: {}", outTradeNo);
        HashMap hashMap = new HashMap();
        hashMap.put("type",1);//1表示来单提醒2表示客户催单
        hashMap.put("orderId",orders.getId());
        hashMap.put("content","订单号:"+orders.getNumber());

        String json = JSON.toJSONString(hashMap);
        webSocketServer.sendToAllClient(json);

    }

    /**
     * 订单催单
     * @param id
     */
    @Override
    public void reminder(Long id) {
       // 查询订单
       Orders orders = orderMapper.getById(id);
       // 判断订单是否为待派单状态
        if (!Objects.equals(orders.getStatus(), Orders.TO_BE_CONFIRMED)) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }

        HashMap hashMap = new HashMap();
        hashMap.put("type",2);//1表示来单提醒2表示客户催单
        hashMap.put("orderId",id);
        hashMap.put("content","订单号:"+orders.getNumber());
        String json = JSON.toJSONString(hashMap);
        webSocketServer.sendToAllClient(json);//发送消息给客户端
    }

    private String getOrderDishesString(List<OrderDetail> orderDetailList) {
        if (orderDetailList == null || orderDetailList.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < orderDetailList.size(); i++) {
            OrderDetail detail = orderDetailList.get(i);
            sb.append(detail.getName());
            sb.append(" x").append(detail.getNumber());

            if (i < orderDetailList.size() - 1) {
                sb.append(", ");
            }
        }

        return sb.toString();
    }
    private String buildFullAddress(AddressBook addressBook) {
        StringBuilder address = new StringBuilder();

        // 拼接省级名称（跳过"市辖区"等无意义字段）
        if (addressBook.getProvinceName() != null
                && !addressBook.getProvinceName().equals("市辖区")) {
            address.append(addressBook.getProvinceName());
        }

        // 拼接市级名称（跳过"市辖区"等无意义字段）
        if (addressBook.getCityName() != null
                && !addressBook.getCityName().equals("市辖区")) {
            address.append(addressBook.getCityName());
        }

        // 拼接区级名称
        if (addressBook.getDistrictName() != null
                && !addressBook.getDistrictName().equals("市辖区")) {
            address.append(addressBook.getDistrictName());
        }

        // 拼接详细地址（限制长度，避免超限）
        if (addressBook.getDetail() != null) {
            String detail = addressBook.getDetail();
            // 如果详细地址超过30个字符，截取前30个
            if (detail.length() > 30) {
                detail = detail.substring(0, 30);
            }
            address.append(detail);
        }

        String fullAddress = address.toString();
        log.info("拼接后的用户地址: {} (长度: {})", fullAddress, fullAddress.length());

        return fullAddress;
    }
}


