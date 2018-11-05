package com.pinyougou.order.service.impl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.pinyougou.mapper.TbItemMapper;
import com.pinyougou.mapper.TbOrderItemMapper;
import com.pinyougou.order.service.OrderService;
import com.pinyougou.pojo.TbOrderItem;
import com.pinyougou.pojo.TbPayLog;
import com.pinyougou.pojo.TbPayLogExample;
import com.pinyougou.pojogroup.Cart;
import org.springframework.beans.factory.annotation.Autowired;
import com.alibaba.dubbo.config.annotation.Service;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.pinyougou.mapper.TbOrderMapper;
import com.pinyougou.mapper.TbPayLogMapper;
import com.pinyougou.pojo.TbItem;
import com.pinyougou.pojo.TbOrder;
import com.pinyougou.pojo.TbOrderExample;
import com.pinyougou.pojo.TbOrderExample.Criteria;

import entity.PageResult;
import org.springframework.data.redis.core.RedisTemplate;
import util.IdWorker;

/**
 * 服务实现层
 *
 * @author Administrator
 */
@Service
public class OrderServiceImpl implements OrderService {

	@Autowired
	private TbOrderMapper orderMapper;

	@Autowired
	private RedisTemplate redisTemplate;

	@Autowired
	private IdWorker idWorker;

	@Autowired
	private TbOrderItemMapper orderItemMapper;

	@Autowired
	private TbItemMapper itemMapper;

	@Autowired
	private TbPayLogMapper payLogMapper;

	/**
	 * 查询全部
	 */
	@Override
	public List<TbOrder> findAll() {
		return orderMapper.selectByExample(null);
	}

	/**
	 * 按分页查询
	 */
	@Override
	public PageResult findPage(int pageNum, int pageSize) {
		PageHelper.startPage(pageNum, pageSize);
		Page<TbOrder> page = (Page<TbOrder>) orderMapper.selectByExample(null);
		return new PageResult(page.getTotal(), page.getResult());
	}

	/**
	 * 增加
	 */
	@Override
	public void add(TbOrder order) {
		// 从缓存中获得购物车数据
		List<Cart> cartList = (List<Cart>) redisTemplate.boundHashOps("cartList").get(order.getUserId());

		double totalMoney = 0;//总金额
		List<String> orderIdList=new ArrayList();//订单ID列表
		for (Cart cart : cartList) {
			long orderId = idWorker.nextId();
			System.out.println("sellerId:" + cart.getSellerId());
			// 每个商家一个order
			TbOrder tbOrder = new TbOrder();
			tbOrder.setOrderId(orderId);// 订单ID
			tbOrder.setUserId(order.getUserId());// 用户名
			tbOrder.setPaymentType(order.getPaymentType());// 支付类型
			tbOrder.setStatus("1");// 状态：未付款
			tbOrder.setCreateTime(new Date());// 订单创建日期
			tbOrder.setUpdateTime(new Date());// 订单更新日期
			tbOrder.setReceiverAreaName(order.getReceiverAreaName());// 地址
			tbOrder.setReceiverMobile(order.getReceiverMobile());// 手机号
			tbOrder.setReceiver(order.getReceiver());// 收货人
			tbOrder.setSourceType(order.getSourceType());// 订单来源
			tbOrder.setSellerId(cart.getSellerId());// 商家ID

			double money = 0;
			// 循环购物车明细
			for (TbOrderItem orderItem : cart.getOrderItemList()) {
				orderItem.setId(idWorker.nextId());
				orderItem.setOrderId(orderId);// 订单ID
				orderItem.setSellerId(cart.getSellerId());
				// !!!不能从redis中或者前台传过来的价格，要从数据库里查，如果有人更改就完了啊
				Long itemId = orderItem.getItemId();
				TbItem item = itemMapper.selectByPrimaryKey(itemId);
				money += item.getPrice().doubleValue() * orderItem.getNum();// 金额累加
				orderItemMapper.insert(orderItem);
			}
			//累计总金额
			totalMoney += money;
			//添加订单id
			orderIdList.add(orderId+"");
			tbOrder.setPayment(new BigDecimal(money));
			orderMapper.insert(tbOrder);
		}
		
		if("1".equals(order.getPaymentType())) {//如果是微信支付
			TbPayLog payLog = new TbPayLog();//新建支付日志
			String out_trade_no = idWorker.nextId()+"";//支付订单号
			payLog.setOutTradeNo(out_trade_no);
			payLog.setCreateTime(new Date());//创建时间
			String ids=orderIdList.toString().replace("[", "").replace("]", "").replace(" ", "");//订单编号列表
			payLog.setOrderList(ids);
			payLog.setPayType("1");
			payLog.setTotalFee((long)(totalMoney*100));
			payLog.setUserId(order.getUserId());
			payLog.setTradeState("0");
			//插入到数据库中
			payLogMapper.insert(payLog);
			//保存到缓存中
			redisTemplate.boundHashOps("payLog").put(order.getUserId(), payLog);
		}
		// 从缓存中删除购物车数据
		redisTemplate.boundHashOps("cartList").delete(order.getUserId());
	}

	/**
	 * 修改
	 */
	@Override
	public void update(TbOrder order) {
		orderMapper.updateByPrimaryKey(order);
	}

	/**
	 * 根据ID获取实体
	 *
	 * @param id
	 * @return
	 */
	@Override
	public TbOrder findOne(Long id) {
		return orderMapper.selectByPrimaryKey(id);
	}

	/**
	 * 批量删除
	 */
	@Override
	public void delete(Long[] ids) {
		for (Long id : ids) {
			orderMapper.deleteByPrimaryKey(id);
		}
	}

	@Override
	public PageResult findPage(TbOrder order, int pageNum, int pageSize) {
		PageHelper.startPage(pageNum, pageSize);

		TbOrderExample example = new TbOrderExample();
		Criteria criteria = example.createCriteria();

		if (order != null) {
			if (order.getPaymentType() != null && order.getPaymentType().length() > 0) {
				criteria.andPaymentTypeLike("%" + order.getPaymentType() + "%");
			}
			if (order.getPostFee() != null && order.getPostFee().length() > 0) {
				criteria.andPostFeeLike("%" + order.getPostFee() + "%");
			}
			if (order.getStatus() != null && order.getStatus().length() > 0) {
				criteria.andStatusLike("%" + order.getStatus() + "%");
			}
			if (order.getShippingName() != null && order.getShippingName().length() > 0) {
				criteria.andShippingNameLike("%" + order.getShippingName() + "%");
			}
			if (order.getShippingCode() != null && order.getShippingCode().length() > 0) {
				criteria.andShippingCodeLike("%" + order.getShippingCode() + "%");
			}
			if (order.getUserId() != null && order.getUserId().length() > 0) {
				criteria.andUserIdLike("%" + order.getUserId() + "%");
			}
			if (order.getBuyerMessage() != null && order.getBuyerMessage().length() > 0) {
				criteria.andBuyerMessageLike("%" + order.getBuyerMessage() + "%");
			}
			if (order.getBuyerNick() != null && order.getBuyerNick().length() > 0) {
				criteria.andBuyerNickLike("%" + order.getBuyerNick() + "%");
			}
			if (order.getBuyerRate() != null && order.getBuyerRate().length() > 0) {
				criteria.andBuyerRateLike("%" + order.getBuyerRate() + "%");
			}
			if (order.getReceiverAreaName() != null && order.getReceiverAreaName().length() > 0) {
				criteria.andReceiverAreaNameLike("%" + order.getReceiverAreaName() + "%");
			}
			if (order.getReceiverMobile() != null && order.getReceiverMobile().length() > 0) {
				criteria.andReceiverMobileLike("%" + order.getReceiverMobile() + "%");
			}
			if (order.getReceiverZipCode() != null && order.getReceiverZipCode().length() > 0) {
				criteria.andReceiverZipCodeLike("%" + order.getReceiverZipCode() + "%");
			}
			if (order.getReceiver() != null && order.getReceiver().length() > 0) {
				criteria.andReceiverLike("%" + order.getReceiver() + "%");
			}
			if (order.getInvoiceType() != null && order.getInvoiceType().length() > 0) {
				criteria.andInvoiceTypeLike("%" + order.getInvoiceType() + "%");
			}
			if (order.getSourceType() != null && order.getSourceType().length() > 0) {
				criteria.andSourceTypeLike("%" + order.getSourceType() + "%");
			}
			if (order.getSellerId() != null && order.getSellerId().length() > 0) {
				criteria.andSellerIdLike("%" + order.getSellerId() + "%");
			}
		}
		Page<TbOrder> page = (Page<TbOrder>) orderMapper.selectByExample(example);
		return new PageResult(page.getTotal(), page.getResult());
	}

	@Override
	public TbPayLog searchPayLogFromRedis(String userId) {
		return (TbPayLog) redisTemplate.boundHashOps("payLog").get(userId);
	}

	@Override
	public void updateOrderStatus(String out_trade_no, String transaction_id) {
		//修改支付日志状态
		TbPayLog payLog = payLogMapper.selectByPrimaryKey(out_trade_no);
		payLog.setPayTime(new Date());//支付时间
		payLog.setTradeState("1");//已支付
		payLog.setTransactionId(transaction_id);
		payLogMapper.updateByPrimaryKey(payLog);
		//修改订单状态
		String orderIds = payLog.getOrderList();
		String[] ids = orderIds.split(",");
		//遍历ids
		for (String orderId : ids) {
			TbOrder order = orderMapper.selectByPrimaryKey(Long.parseLong(orderId));
			if(order!=null) {//非空判断
				order.setStatus("2");
				orderMapper.updateByPrimaryKey(order);//更新
			}
		}
		//清除payLog缓存
		redisTemplate.boundHashOps("payLog").delete(payLog.getUserId());
	}
}
