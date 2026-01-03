package fun.cyhgraph.service.serviceImpl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import fun.cyhgraph.constant.MessageConstant;
import fun.cyhgraph.context.BaseContext;
import fun.cyhgraph.dto.*;
import fun.cyhgraph.entity.*;
import fun.cyhgraph.exception.AddressBookBusinessException;
import fun.cyhgraph.exception.OrderBusinessException;
import fun.cyhgraph.exception.ShoppingCartBusinessException;
import fun.cyhgraph.mapper.*;
import fun.cyhgraph.result.PageResult;
import fun.cyhgraph.service.OrderService;
import fun.cyhgraph.utils.HttpClientUtil;
import fun.cyhgraph.utils.WeChatPayUtil;
import fun.cyhgraph.vo.OrderPaymentVO;
import fun.cyhgraph.vo.OrderStatisticsVO;
import fun.cyhgraph.vo.OrderSubmitVO;
import fun.cyhgraph.vo.OrderVO;
import fun.cyhgraph.websocket.WebSocketServer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class OrderServiceImpl implements OrderService {

    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private OrderDetailMapper orderDetailMapper;
    @Autowired
    private CartMapper cartMapper;
    @Autowired
    private AddressBookMapper addressBookMapper;
    @Autowired
    private WeChatPayUtil weChatPayUtil;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private UtableInfoMapper utableInfoMapper;

    private Order order;

    @Autowired
    private WebSocketServer webSocketServer;

    @Value("${hanye.shop.address}")
    private String shopAddress;
    @Value("${hanye.baidu.ak}")
    private String ak;

    // 店铺地址ID常量
    private static final Integer SHOP_ADDRESS_ID = 999;

    /**
     * 用户下单
     *
     * @param orderSubmitDTO
     * @return
     */
    @Override
    @Transactional
    public OrderSubmitVO submit(OrderSubmitDTO orderSubmitDTO) {
        log.info("用户下单开始，参数: {}", JSON.toJSONString(orderSubmitDTO));

        // 1、校验就餐方式和餐桌
        validateDiningTypeAndTable(orderSubmitDTO);

        // 2、查询校验地址情况
        AddressBook addressBook = handleAddress(orderSubmitDTO);

        // 3、查询校验购物车情况
        List<Cart> cartList = validateAndGetCartItems();

        // 4、构建订单数据
        Order order = buildOrder(orderSubmitDTO, addressBook, cartList);
        this.order = order;

        // 5、向订单表插入1条数据
        orderMapper.insert(order);
        log.info("订单创建成功，订单ID: {}, 订单号: {}", order.getId(), order.getNumber());

        // 6、向明细表插入n条数据
        insertOrderDetails(order.getId(), cartList);

        // 7、如果是堂食订单，预占餐桌（可选：这里可以先不占用，等支付成功再占用）
        if (orderSubmitDTO.getDiningType() != null &&
                orderSubmitDTO.getDiningType().equals(Order.DINE_IN) &&
                orderSubmitDTO.getTableId() != null) {
            // 这里可以选择预占（状态改为已预订）或者等待支付成功再占用
            // 为了更好的用户体验，我们先预占餐桌，避免支付期间被其他人占用
            try {
                utableInfoMapper.updateStatus(orderSubmitDTO.getTableId(), 3); // 3表示已预订
                log.info("餐桌预占成功，tableId: {}, 状态更新为已预订", orderSubmitDTO.getTableId());
            } catch (Exception e) {
                log.error("餐桌预占失败，tableId: {}", orderSubmitDTO.getTableId(), e);
                // 如果预占失败，可以回滚订单创建，或者继续让用户支付，支付时再检查
            }
        }

        // 8、清理购物车中的数据
        cartMapper.delete(BaseContext.getCurrentId());

        // 9、封装返回结果
        return buildOrderSubmitVO(order);
    }

    /**
     * 校验就餐方式和餐桌
     */
    private void validateDiningTypeAndTable(OrderSubmitDTO orderSubmitDTO) {
        log.info("校验就餐方式和餐桌，diningType: {}, tableId: {}",
                orderSubmitDTO.getDiningType(), orderSubmitDTO.getTableId());

        // 堂食必须选择餐桌
        if (orderSubmitDTO.getDiningType() != null &&
                orderSubmitDTO.getDiningType().equals(Order.DINE_IN)) {

            if (orderSubmitDTO.getTableId() == null) {
                throw new OrderBusinessException("请选择餐桌");
            }

            // 检查餐桌是否存在且空闲
            TableInfo table = utableInfoMapper.selectById(orderSubmitDTO.getTableId());
            if (table == null) {
                throw new OrderBusinessException("餐桌不存在");
            }

            // 检查餐桌状态：1-空闲，2-占用中，3-已预订，4-停用
            if (table.getStatus() == 2 || table.getStatus() == 4) {
                throw new OrderBusinessException("该餐桌当前不可用");
            }

            // 如果是已预订状态，检查是否是当前用户预订的（可以通过缓存或预留字段实现）
            // 这里简化处理：已预订状态也不可用
            if (table.getStatus() == 3) {
                throw new OrderBusinessException("该餐桌已被预订");
            }

            // 检查是否有未完成的订单正在使用此餐桌
            Integer activeOrders = orderMapper.countActiveOrdersByTableId(orderSubmitDTO.getTableId());
            if (activeOrders > 0) {
                throw new OrderBusinessException("该餐桌已有订单正在进行中");
            }
        }
    }

    /**
     * 处理地址信息
     */
    private AddressBook handleAddress(OrderSubmitDTO orderSubmitDTO) {
        // 如果是堂食，使用店铺地址
        if (orderSubmitDTO.getDiningType() != null &&
                orderSubmitDTO.getDiningType().equals(Order.DINE_IN)) {

            AddressBook shopAddress = new AddressBook();
            shopAddress.setId(SHOP_ADDRESS_ID);
            shopAddress.setDetail("味臻坊餐厅（在店就餐）");
            shopAddress.setConsignee("在店就餐顾客");
            shopAddress.setPhone("00000000000");
            shopAddress.setProvinceName("餐厅");
            shopAddress.setCityName("店内");
            shopAddress.setDistrictName("就餐区");
            return shopAddress;
        } else {
            // 外卖：正常校验地址
            AddressBook addressBook = addressBookMapper.getById(orderSubmitDTO.getAddressId());
            if (addressBook == null) {
                throw new AddressBookBusinessException(MessageConstant.ADDRESS_BOOK_IS_NULL);
            }

            // 检查是否超出配送范围（根据需求开启）
            // checkOutOfRange(addressBook.getCityName() + addressBook.getDistrictName() + addressBook.getDetail());

            return addressBook;
        }
    }

    /**
     * 校验并获取购物车商品
     */
    private List<Cart> validateAndGetCartItems() {
        Integer userId = BaseContext.getCurrentId();
        Cart cart = new Cart();
        cart.setUserId(userId);
        List<Cart> cartList = cartMapper.list(cart);

        if (cartList == null || cartList.isEmpty()) {
            throw new ShoppingCartBusinessException(MessageConstant.CART_IS_NULL);
        }

        return cartList;
    }

    /**
     * 构建订单对象
     */
    private Order buildOrder(OrderSubmitDTO orderSubmitDTO, AddressBook addressBook, List<Cart> cartList) {
        Order order = new Order();
        BeanUtils.copyProperties(orderSubmitDTO, order);

        // 设置地址信息
        order.setAddressBookId(addressBook.getId());
        order.setPhone(addressBook.getPhone());
        order.setAddress(addressBook.getDetail());
        order.setConsignee(addressBook.getConsignee());

        // 设置餐桌信息
        if (orderSubmitDTO.getDiningType() != null) {
            order.setDiningType(orderSubmitDTO.getDiningType());
            order.setTableId(orderSubmitDTO.getTableId());
            order.setTableNumber(orderSubmitDTO.getTableNumber());
        } else {
            order.setDiningType(Order.DELIVERY); // 默认外卖
        }

        // 设置订单基础信息
        order.setNumber(String.valueOf(System.currentTimeMillis()));
        order.setUserId(BaseContext.getCurrentId());
        order.setStatus(Order.PENDING_PAYMENT); // 待付款
        order.setPayStatus(Order.UN_PAID); // 未支付
        order.setOrderTime(LocalDateTime.now());

        // 计算总金额（订单中已有amount字段，这里可以再次验证）
        double totalAmount = cartList.stream()
                .mapToDouble(c -> c.getAmount().doubleValue() * c.getNumber())
                .sum();

        // 加上打包费和配送费（如果前端已经计算好，直接使用）
        if (orderSubmitDTO.getAmount() == null) {
            BigDecimal deliveryFee = (orderSubmitDTO.getDiningType() != null &&
                    orderSubmitDTO.getDiningType().equals(Order.DINE_IN)) ?
                    BigDecimal.ZERO : new BigDecimal("6");

            BigDecimal packFee = new BigDecimal(cartList.size()); // 每件商品1元打包费

            order.setAmount(new BigDecimal(totalAmount)
                    .add(deliveryFee)
                    .add(packFee));

            // 设置打包费
            order.setPackAmount(cartList.size());
        }

        return order;
    }

    /**
     * 插入订单明细
     */
    private void insertOrderDetails(Integer orderId, List<Cart> cartList) {
        List<OrderDetail> orderDetailList = new ArrayList<>();
        for (Cart cart : cartList) {
            OrderDetail orderDetail = new OrderDetail();
            BeanUtils.copyProperties(cart, orderDetail);
            orderDetail.setOrderId(orderId);
            orderDetailList.add(orderDetail);
        }
        orderDetailMapper.insertBatch(orderDetailList);
    }

    /**
     * 构建订单提交返回结果
     */
    private OrderSubmitVO buildOrderSubmitVO(Order order) {
        return OrderSubmitVO.builder()
                .id(order.getId())
                .orderNumber(order.getNumber())
                .orderAmount(order.getAmount())
                .orderTime(order.getOrderTime())
                .build();
    }

    /**
     * 当前用户未支付订单数量
     *
     * @return
     */
    @Override
    public Integer unPayOrderCount() {
        Integer userId = BaseContext.getCurrentId();
        return orderMapper.getUnPayCount(userId);
    }

    /**
     * 根据id查询订单详情
     *
     * @param id
     * @return
     */
    @Override
    public OrderVO getById(Integer id) {
        Order order = orderMapper.getById(id);
        List<OrderDetail> orderDetailList = orderDetailMapper.getById(id);
        OrderVO orderVO = new OrderVO();
        BeanUtils.copyProperties(order, orderVO);
        orderVO.setOrderDetailList(orderDetailList);
        return orderVO;
    }

    /**
     * 用户端条件分页查询历史订单
     *
     * @param page
     * @param pageSize
     * @param status
     * @return
     */
    @Override
    public PageResult userPage(int page, int pageSize, Integer status) {
        PageHelper.startPage(page, pageSize);
        OrderPageDTO orderPageDTO = new OrderPageDTO();
        orderPageDTO.setUserId(BaseContext.getCurrentId());
        orderPageDTO.setStatus(status);
        Page<Order> orderPage = orderMapper.page(orderPageDTO);

        List<OrderVO> list = new ArrayList<>();
        if (orderPage != null && orderPage.getTotal() > 0) {
            for (Order order : orderPage) {
                Integer orderId = order.getId();
                List<OrderDetail> orderDetails = orderDetailMapper.getById(orderId);
                OrderVO orderVO = new OrderVO();
                BeanUtils.copyProperties(order, orderVO);
                orderVO.setOrderDetailList(orderDetails);
                list.add(orderVO);
            }
        }
        return new PageResult(orderPage.getTotal(), list);
    }

    /**
     * 用户根据订单id取消订单
     *
     * @param id
     */
    @Override
    @Transactional
    public void userCancelById(Integer id) throws Exception {
        log.info("用户取消订单，订单ID: {}", id);

        // 根据id查询订单
        Order ordersDB = orderMapper.getById(id);
        if (ordersDB == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }

        // 订单状态 1待付款 2待接单 3已接单 4派送中 5已完成 6已取消
        if (ordersDB.getStatus() > 2) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        Order order = new Order();
        order.setId(ordersDB.getId());

        // 如果订单已支付且处于待接单状态，需要进行退款
        if (ordersDB.getPayStatus().equals(Order.PAID) &&
                ordersDB.getStatus().equals(Order.TO_BE_CONFIRMED)) {
            // 调用微信支付退款接口（如果有的话）
            // weChatPayUtil.refund(...);
            order.setPayStatus(Order.REFUND);
        }

        // 释放餐桌（如果是堂食订单）
        if (ordersDB.getDiningType() != null &&
                ordersDB.getDiningType().equals(Order.DINE_IN) &&
                ordersDB.getTableId() != null) {

            utableInfoMapper.updateStatus(ordersDB.getTableId(), 1); // 恢复为空闲状态
            log.info("订单取消，释放餐桌，tableId: {}", ordersDB.getTableId());
        }

        // 更新订单状态
        order.setStatus(Order.CANCELLED);
        order.setCancelReason("用户取消");
        order.setCancelTime(LocalDateTime.now());
        orderMapper.update(order);
    }

    /**
     * 根据订单id再来一单
     *
     * @param id
     */
    @Override
    @Transactional
    public void reOrder(Integer id) {
        Integer userId = BaseContext.getCurrentId();
        List<OrderDetail> detailList = orderDetailMapper.getById(id);

        List<Cart> cartList = detailList.stream().map(x -> {
            Cart cart = new Cart();
            BeanUtils.copyProperties(x, cart, "id");
            cart.setUserId(userId);
            cart.setCreateTime(LocalDateTime.now());
            return cart;
        }).toList();

        cartMapper.insertBatch(cartList);
    }

    /**
     * 用户支付订单
     *
     * @param orderPaymentDTO
     * @return
     */
    @Override
    @Transactional
    public OrderPaymentVO payment(OrderPaymentDTO orderPaymentDTO) {
        log.info("用户支付订单开始，参数: {}", JSON.toJSONString(orderPaymentDTO));

        // 当前登录用户id
        Integer userId = BaseContext.getCurrentId();
        User user = userMapper.getById(userId);

        // 调用微信支付接口，生成预支付交易单
        // 这里简化为直接支付成功
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("code", "ORDERPAID");
        OrderPaymentVO vo = jsonObject.toJavaObject(OrderPaymentVO.class);
        vo.setPackageStr(jsonObject.getString("package"));

        // 更新订单状态为已支付和待接单
        Integer orderPaidStatus = Order.PAID;
        Integer orderStatus = Order.TO_BE_CONFIRMED;
        LocalDateTime checkOutTime = LocalDateTime.now();
        orderMapper.updateStatus(orderStatus, orderPaidStatus, checkOutTime, this.order.getId());

        // 支付成功后处理餐桌占用
        afterPaymentSuccess(this.order.getId());

        // 发送WebSocket消息给商家端
        sendOrderNotification(this.order.getId());

        return vo;
    }

    /**
     * 支付成功后处理餐桌占用
     */
    private void afterPaymentSuccess(Integer orderId) {
        Order order = orderMapper.getById(orderId);
        if (order == null) {
            log.error("支付成功回调：订单不存在，orderId: {}", orderId);
            return;
        }

        log.info("支付成功回调处理，订单ID: {}, 就餐方式: {}, 餐桌ID: {}",
                orderId, order.getDiningType(), order.getTableId());

        // 如果是堂食订单且选择了餐桌
        if (order.getDiningType() != null &&
                order.getDiningType().equals(Order.DINE_IN) &&
                order.getTableId() != null) {

            try {
                // 检查餐桌状态
                TableInfo table = utableInfoMapper.selectById(order.getTableId());
                if (table == null) {
                    log.error("支付成功：餐桌不存在，tableId: {}", order.getTableId());
                    // 可以发送通知给商家处理异常
                    return;
                }

                // 如果餐桌是预订状态（3）或者空闲状态（1），可以占用
                if (table.getStatus() == 1 || table.getStatus() == 3) {
                    // 更新餐桌状态为占用中
                    utableInfoMapper.updateStatus(order.getTableId(), 2);
                    log.info("支付成功：餐桌状态更新为占用中，tableId: {}, 订单ID: {}",
                            order.getTableId(), orderId);
                } else if (table.getStatus() == 2) {
                    // 如果已经是占用中状态，可能是重复支付或其他异常
                    log.warn("支付成功：餐桌已被占用，tableId: {}, 当前状态: {}",
                            order.getTableId(), table.getStatus());
                    // 可以发送通知给商家处理
                } else {
                    log.error("支付成功：餐桌不可用，tableId: {}, 状态: {}",
                            order.getTableId(), table.getStatus());
                    // 需要退款或处理异常
                }
            } catch (Exception e) {
                log.error("支付成功处理餐桌占用失败，tableId: {}, orderId: {}",
                        order.getTableId(), orderId, e);
            }
        }
    }

    /**
     * 发送订单通知给商家端
     */
    private void sendOrderNotification(Integer orderId) {
        try {
            Order order = orderMapper.getById(orderId);
            Map<String, Object> map = new HashMap<>();
            map.put("type", 1); // 1表示来单提醒
            map.put("orderId", orderId);
            map.put("content", "订单号：" + order.getNumber());

            // 如果是堂食订单，添加餐桌信息
            if (order.getDiningType() != null &&
                    order.getDiningType().equals(Order.DINE_IN)) {
                map.put("diningType", "堂食");
                map.put("tableNumber", order.getTableNumber());
            } else {
                map.put("diningType", "外卖");
            }

            String json = JSON.toJSONString(map);
            log.info("发送订单通知给商家端：{}", map);
            webSocketServer.sendToAllClient(json);
        } catch (Exception e) {
            log.error("发送订单通知失败", e);
        }
    }

    /**
     * 条件分页查询订单信息
     *
     * @param orderPageDTO
     * @return
     */
    @Override
    public PageResult conditionSearch(OrderPageDTO orderPageDTO) {
        PageHelper.startPage(orderPageDTO.getPage(), orderPageDTO.getPageSize());
        Page<Order> orders = orderMapper.page(orderPageDTO);
        List<OrderVO> orderVOList = getOrderVOList(orders);
        return new PageResult(orders.getTotal(), orderVOList);
    }

    /**
     * 不同状态订单数量统计
     *
     * @return
     */
    @Override
    public OrderStatisticsVO statistics() {
        Integer toBeConfirmed = orderMapper.countByStatus(Order.TO_BE_CONFIRMED);
        Integer confirmed = orderMapper.countByStatus(Order.CONFIRMED);
        Integer deliveryInProgress = orderMapper.countByStatus(Order.DELIVERY_IN_PROGRESS);

        return OrderStatisticsVO.builder()
                .toBeConfirmed(toBeConfirmed)
                .confirmed(confirmed)
                .deliveryInProgress(deliveryInProgress)
                .build();
    }

    /**
     * 接单
     *
     * @param orderConfirmDTO
     */
    @Override
    @Transactional
    public void confirm(OrderConfirmDTO orderConfirmDTO) {
        Order order = Order.builder()
                .id(orderConfirmDTO.getId())
                .status(Order.CONFIRMED)
                .build();
        orderMapper.update(order);
    }

    /**
     * 拒单
     *
     * @param orderRejectionDTO
     */
    @Override
    @Transactional
    public void reject(OrderRejectionDTO orderRejectionDTO) {
        Integer orderId = orderRejectionDTO.getId();
        Order orderDB = orderMapper.getById(orderId);

        if (orderDB == null || !orderDB.getStatus().equals(Order.TO_BE_CONFIRMED)) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        Order order = new Order();
        order.setId(orderDB.getId());
        order.setPayStatus(Order.REFUND);
        order.setStatus(Order.CANCELLED);
        order.setRejectionReason(orderRejectionDTO.getRejectionReason());
        order.setCancelTime(LocalDateTime.now());

        // 如果是堂食订单，释放餐桌
        if (orderDB.getDiningType() != null &&
                orderDB.getDiningType().equals(Order.DINE_IN) &&
                orderDB.getTableId() != null) {
            utableInfoMapper.updateStatus(orderDB.getTableId(), 1);
        }

        orderMapper.update(order);
    }

    /**
     * 取消订单
     *
     * @param orderCancelDTO
     */
    @Override
    @Transactional
    public void cancel(OrderCancelDTO orderCancelDTO) {
        Integer orderId = orderCancelDTO.getId();
        Order orderDB = orderMapper.getById(orderId);

        Order order = new Order();
        order.setId(orderDB.getId());
        order.setPayStatus(Order.REFUND);
        order.setStatus(Order.CANCELLED);
        order.setCancelReason(orderCancelDTO.getCancelReason());
        order.setCancelTime(LocalDateTime.now());

        // 如果是堂食订单，释放餐桌
        if (orderDB.getDiningType() != null &&
                orderDB.getDiningType().equals(Order.DINE_IN) &&
                orderDB.getTableId() != null) {
            utableInfoMapper.updateStatus(orderDB.getTableId(), 1);
        }

        orderMapper.update(order);
    }

    /**
     * 根据id派送订单
     *
     * @param id
     */
    @Override
    @Transactional
    public void delivery(Integer id) {
        Order orderDB = orderMapper.getById(id);
        if (orderDB == null || !orderDB.getStatus().equals(Order.CONFIRMED)) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        Order order = new Order();
        order.setId(orderDB.getId());
        order.setStatus(Order.DELIVERY_IN_PROGRESS);
        orderMapper.update(order);
    }

    /**
     * 完成订单
     *
     * @param id
     */
    @Override
    @Transactional
    public void complete(Integer id) {
        Order orderDB = orderMapper.getById(id);
        if (orderDB == null || !orderDB.getStatus().equals(Order.DELIVERY_IN_PROGRESS)) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        Order order = new Order();
        order.setId(orderDB.getId());
        order.setStatus(Order.COMPLETED);
        order.setDeliveryTime(LocalDateTime.now());

        // 如果是堂食订单，释放餐桌
        if (orderDB.getDiningType() != null &&
                orderDB.getDiningType().equals(Order.DINE_IN) &&
                orderDB.getTableId() != null) {
            utableInfoMapper.updateStatus(orderDB.getTableId(), 1);
            log.info("订单完成，释放餐桌，tableId: {}", orderDB.getTableId());
        }

        orderMapper.update(order);
    }

    /**
     * 用户催单
     * @param id
     */
    @Override
    public void reminder(Integer id) {
        Order orderDB = orderMapper.getById(id);
        if (orderDB == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }

        Map<String, Object> map = new HashMap<>();
        map.put("type", 2); // 2表示客户催单
        map.put("orderId", id);
        map.put("content", "订单号：" + orderDB.getNumber());

        // 添加餐桌信息
        if (orderDB.getDiningType() != null &&
                orderDB.getDiningType().equals(Order.DINE_IN)) {
            map.put("diningType", "堂食");
            map.put("tableNumber", orderDB.getTableNumber());
        }

        String json = JSON.toJSONString(map);
        log.info("发送催单通知给商家端：{}", map);
        webSocketServer.sendToAllClient(json);
    }

    /**
     * 获取订单VO列表
     */
    private List<OrderVO> getOrderVOList(Page<Order> page) {
        List<OrderVO> orderVOList = new ArrayList<>();
        List<Order> ordersList = page.getResult();
        if (!CollectionUtils.isEmpty(ordersList)) {
            for (Order orders : ordersList) {
                OrderVO orderVO = new OrderVO();
                BeanUtils.copyProperties(orders, orderVO);
                String orderDishes = getOrderDishesStr(orders);
                orderVO.setOrderDishes(orderDishes);
                orderVOList.add(orderVO);
            }
        }
        return orderVOList;
    }

    /**
     * 获取订单菜品信息字符串
     */
    private String getOrderDishesStr(Order order) {
        List<OrderDetail> orderDetailList = orderDetailMapper.getById(order.getId());
        List<String> orderDishList = orderDetailList.stream().map(x -> {
            return x.getName() + "*" + x.getNumber() + ";";
        }).collect(Collectors.toList());
        return String.join("", orderDishList);
    }

    /**
     * 检查客户的收货地址是否超出配送范围
     */
    private void checkOutOfRange(String address) {
        Map<String, String> map = new HashMap<>();
        map.put("address", shopAddress);
        map.put("output", "json");
        map.put("ak", ak);

        String shopCoordinate = HttpClientUtil.doGet("https://api.map.baidu.com/geocoding/v3", map);
        JSONObject jsonObject = JSON.parseObject(shopCoordinate);

        if (!jsonObject.getString("status").equals("0")) {
            throw new OrderBusinessException("店铺地址解析失败");
        }

        JSONObject location = jsonObject.getJSONObject("result").getJSONObject("location");
        String lat = location.getString("lat");
        String lng = location.getString("lng");
        String shopLngLat = lat + "," + lng;

        map.put("address", address);
        String userCoordinate = HttpClientUtil.doGet("https://api.map.baidu.com/geocoding/v3", map);
        jsonObject = JSON.parseObject(userCoordinate);

        if (!jsonObject.getString("status").equals("0")) {
            throw new OrderBusinessException("收货地址解析失败");
        }

        location = jsonObject.getJSONObject("result").getJSONObject("location");
        lat = location.getString("lat");
        lng = location.getString("lng");
        String userLngLat = lat + "," + lng;

        map.put("origin", shopLngLat);
        map.put("destination", userLngLat);
        map.put("steps_info", "0");

        String json = HttpClientUtil.doGet("https://api.map.baidu.com/directionlite/v1/driving", map);
        jsonObject = JSON.parseObject(json);

        if (!jsonObject.getString("status").equals("0")) {
            throw new OrderBusinessException("配送路线规划失败");
        }

        JSONObject result = jsonObject.getJSONObject("result");
        JSONArray jsonArray = (JSONArray) result.get("routes");
        Integer distance = (Integer) ((JSONObject) jsonArray.get(0)).get("distance");

        if (distance > 5000) {
            throw new OrderBusinessException("超出配送范围");
        }
    }
}