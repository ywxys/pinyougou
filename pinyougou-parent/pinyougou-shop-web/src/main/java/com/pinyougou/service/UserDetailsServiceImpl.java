package com.pinyougou.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import com.pinyougou.pojo.TbSeller;
import com.pinyougou.sellergoods.service.SellerService;

public class UserDetailsServiceImpl implements UserDetailsService {

	private SellerService sellerService;

	public void setSellerService(SellerService sellerService) {
		this.sellerService = sellerService;
	}

	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		System.out.println("经过了UserDetailsServiceImpl");
		
		// 构建角色列表
		List<GrantedAuthority> grantAuths = new ArrayList<>();
		grantAuths.add(new SimpleGrantedAuthority("ROLE_SELLER"));
		UserDetails details = new User(username, "123456", grantAuths);
		
		// 得到商家对象
		TbSeller seller = sellerService.findOne(username);
		if (seller != null && "1".equals(seller.getStatus())) {
			// 返回用户对象
			return new User(username, seller.getPassword(), grantAuths);
		} else {
			return null;
		}

	}

}
