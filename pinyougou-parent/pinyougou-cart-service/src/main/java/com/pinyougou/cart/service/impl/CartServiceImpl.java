package com.pinyougou.cart.service.impl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;

import com.alibaba.dubbo.config.annotation.Service;
import com.pinyougou.cart.service.CartService;
import com.pinyougou.mapper.TbItemMapper;
import com.pinyougou.pojo.TbItem;
import com.pinyougou.pojo.TbOrderItem;
import com.pinyougou.pojogroup.Cart;

@Service
public class CartServiceImpl implements CartService {
	@Autowired
	private TbItemMapper itemMapper;

	@Override
	/**
	 * 添加商品到购物车
	 */
	public List<Cart> addGoodsToCartList(List<Cart> cartList, Long itemId, Integer num) {
		// 1.根据商品SKU ID查询SKU商品信息
		TbItem item = itemMapper.selectByPrimaryKey(itemId);
		if (item == null) {
			throw new RuntimeException("商品不存在");
		}
		if (!"1".equals(item.getStatus())) {
			//商品未启用
			throw new RuntimeException("商品状态无效");
		}
		// 2.获取商家ID
		String sellerId = item.getSellerId();
		// 3.根据商家ID判断购物车列表中是否存在该商家的购物车
		Cart cart = findCartBySellerId(cartList, sellerId);
		// 4.如果购物车列表中不存在该商家的购物车
		if (cart == null) {
			// 4.1 新建购物车对象
			cart = new Cart();
			//添加sellerId
			cart.setSellerId(sellerId);
			//添加sellerName
			cart.setSellerName(item.getSeller());
			//新建itemList和orderItem
			TbOrderItem orderItem = createOrderItem(item, num);
			List<TbOrderItem> orderItemList = new ArrayList<>();
			orderItemList.add(orderItem);
			//添加itemList
			cart.setOrderItemList(orderItemList);
			// 4.2 将新建的购物车对象添加到购物车列表
			cartList.add(cart);
		} else {
			// 5.如果购物车列表中存在该商家的购物车
			// 查询购物车明细列表中是否存在该商品
			TbOrderItem orderItem = findItemByItemId(cart.getOrderItemList(), itemId);
			if (orderItem == null) {
				// 5.1. 如果没有，新增购物车明细
				orderItem = createOrderItem(item, num);
				cart.getOrderItemList().add(orderItem);
			} else {
				// 5.2. 如果有，在原购物车明细上添加数量，更改金额
				orderItem.setNum(orderItem.getNum() + num);//更改数量
				orderItem.setTotalFee(new BigDecimal(orderItem.getNum() * orderItem.getPrice().doubleValue()));
				// 如果数量操作后小于等于0，则移除
				if (orderItem.getNum() <= 0) {
					cart.getOrderItemList().remove(orderItem);// 移除购物车明细
				}
				// 如果移除后cart的明细数量为0，则将cart移除
				if (cart.getOrderItemList().size() == 0) {
					cartList.remove(cart);
				}
			}
		}
		return cartList;
	}

	/**
	 * 根据itemId查询orderItem
	 * @param orderItemList
	 * @param itemId
	 * @return
	 */
	private TbOrderItem findItemByItemId(List<TbOrderItem> orderItemList, Long itemId) {
		for (TbOrderItem orderItem : orderItemList) {
			//Long是引用数据类型，需要转化为long进行比较
			if (itemId.longValue() == orderItem.getItemId().longValue()) {
				return orderItem;
			}
		}
		return null;
	}

	/**
	 * 将item封装为orderItem
	 * 
	 * @param item
	 * @param num
	 * @return
	 */
	private TbOrderItem createOrderItem(TbItem item, Integer num) {
		if (num <= 0) {
			throw new RuntimeException("数量非法");
		}
		TbOrderItem orderItem = new TbOrderItem();
		orderItem.setGoodsId(item.getGoodsId());
		orderItem.setItemId(item.getId());
		orderItem.setNum(num);
		orderItem.setPicPath(item.getImage());
		orderItem.setPrice(item.getPrice());
		orderItem.setSellerId(item.getSellerId());
		orderItem.setTitle(item.getTitle());
		orderItem.setTotalFee(new BigDecimal(item.getPrice().doubleValue() * num));
		return orderItem;
	}

	/**
	 * 根据sellerId查询Cart
	 * 
	 * @param cartList
	 * @param sellerId
	 * @return
	 */
	private Cart findCartBySellerId(List<Cart> cartList, String sellerId) {
		for (Cart cart : cartList) {
			if (sellerId.equals(cart.getSellerId())) {
				return cart;
			}
		}
		return null;
	}

	@Autowired
	private RedisTemplate redisTemplate;

	@Override
	public List<Cart> findCartListFromRedis(String username) {
		System.out.println("从redis中取出购物车数据"+username);
		List<Cart> cartList = (List<Cart>) redisTemplate.boundHashOps("cartList").get(username);
		if(cartList==null) {//缓存中没有数据
			cartList = new ArrayList<>();
		}
		return cartList;
	}

	@Override
	public void saveCartListToRedis(String username, List<Cart> cartList) {
		System.out.println("向redis中存入数据"+username);
		redisTemplate.boundHashOps("cartList").put(username, cartList);
	}

	@Override
	public List<Cart> mergeCartList(List<Cart> cartList1, List<Cart> cartList2) {
		System.out.println("合并购物车");
		for (Cart cart : cartList2) {
			for (TbOrderItem orderItem : cart.getOrderItemList()) {
				//使用添加方法将list2的数据加入list1
				cartList1 = addGoodsToCartList(cartList1, orderItem.getItemId(), orderItem.getNum());
			}
		}
		return cartList1;
	}

}
