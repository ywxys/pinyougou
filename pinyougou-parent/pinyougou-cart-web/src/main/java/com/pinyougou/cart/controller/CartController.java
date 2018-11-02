package com.pinyougou.cart.controller;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.pinyougou.cart.service.CartService;
import com.pinyougou.pojogroup.Cart;

import entity.Result;

@RestController
@RequestMapping("/cart")
public class CartController {
	@Reference(timeout = 6000)
	private CartService cartService;

	@Autowired
	private HttpServletRequest request;

	@Autowired
	private HttpServletResponse response;

	/**
	 * 从缓存中获取cartList
	 * 
	 * @return
	 */
	@RequestMapping("/findCartList")
	public List<Cart> findCartList() {
		String username = SecurityContextHolder.getContext().getAuthentication().getName();
		// 从cookie中读取购物车
		String cartListStr = util.CookieUtil.getCookieValue(request, "cartList", "UTF-8");
		if (cartListStr == null || "".equals(cartListStr)) {//cookie中没有数据默认给个空集合的字符串
			cartListStr = "[]";
		}
		List<Cart> cartList_cookie = JSON.parseArray(cartListStr, Cart.class);
		if ("anonymousUser".equals(username)) {
			//用户未登录
			return cartList_cookie;
		} else {
			// 用户已登录,从redis中获取购物车数据
			List<Cart>cartList_redis = cartService.findCartListFromRedis(username);
			if(cartList_cookie.size()>0) {
				//cookie的购物车非空
				//合并购物车
				cartList_redis = cartService.mergeCartList(cartList_cookie, cartList_redis);
				//将数据重新存入redis中
				cartService.saveCartListToRedis(username, cartList_redis);
				//删除缓存数据
				util.CookieUtil.deleteCookie(request, response, "cartList");
			}
			return cartList_redis;
		}
	}

	@RequestMapping("/addGoodsToCartList")
	public Result addGoodsToCartList(Long itemId, Integer num) {
		try {
			// 获取登录名
			String username = SecurityContextHolder.getContext().getAuthentication().getName();
			List<Cart> cartList = findCartList();// 获取购物车列表
			cartList = cartService.addGoodsToCartList(cartList, itemId, num);
			if ("anonymousUser".equals(username)) {
				// 未登录,将购物车数据存入cookie
				util.CookieUtil.setCookie(request, response, "cartList", JSON.toJSONString(cartList), 3600 * 24,
						"UTF-8");
				System.out.println("向cookie存入数据");
			} else {
				// 已登录,存入redis
				cartService.saveCartListToRedis(username, cartList);
			}
			return new Result(true, "添加成功");
		} catch (RuntimeException e) {
			e.printStackTrace();
			return new Result(false, e.getMessage());
		} catch (Exception e) {
			return new Result(false, "添加失败");
		}

	}
}
