package com.pinyougou.page.controller;

import java.util.Map;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import com.alibaba.dubbo.config.annotation.Reference;
import com.pinyougou.page.service.ItemPageService;

@Controller
public class PageController {
	@Reference
	private ItemPageService itempageService;
	
	@RequestMapping("/{id}")
	public ModelAndView itemPage(@PathVariable Long id) {
		ModelAndView mv = new ModelAndView("item");
		Map dataModel = itempageService.getItemMap(id);
		mv.getModel().putAll(dataModel);
		return mv;
	}
}
