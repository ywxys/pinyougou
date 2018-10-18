package com.pinyougou.sellergoods.service.impl;

import java.util.Date;
import java.util.List;
import java.util.Map;

import com.alibaba.fastjson.JSON;
import com.pinyougou.mapper.*;
import com.pinyougou.pojo.TbItem;
import com.pinyougou.pojo.TbItemExample;
import com.pinyougou.pojogroup.Goods;
import org.springframework.beans.factory.annotation.Autowired;
import com.alibaba.dubbo.config.annotation.Service;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.pinyougou.pojo.TbGoods;
import com.pinyougou.pojo.TbGoodsExample;
import com.pinyougou.pojo.TbGoodsExample.Criteria;
import com.pinyougou.sellergoods.service.GoodsService;

import entity.PageResult;
import org.springframework.transaction.annotation.Transactional;

/**
 * 服务实现层
 *
 * @author Administrator
 */
@Service
@Transactional
public class GoodsServiceImpl implements GoodsService {

	@Autowired
	private TbGoodsMapper goodsMapper;

	@Autowired
	private TbGoodsDescMapper goodsDescMapper;

	@Autowired
	private TbItemMapper itemMapper;

	@Autowired
	private TbItemCatMapper itemCatMapper;

	@Autowired
	private TbBrandMapper brandMapper;

	@Autowired
	private TbSellerMapper sellerMapper;

	/**
	 * 查询全部
	 */
	@Override
	public List<TbGoods> findAll() {
		return goodsMapper.selectByExample(null);
	}

	/**
	 * 按分页查询
	 */
	@Override
	public PageResult findPage(int pageNum, int pageSize) {
		PageHelper.startPage(pageNum, pageSize);
		Page<TbGoods> page = (Page<TbGoods>) goodsMapper.selectByExample(null);
		return new PageResult(page.getTotal(), page.getResult());
	}

	/**
	 * 增加
	 */
	@Override
	public void add(Goods goods) {
		TbGoods tbGoods = goods.getGoods();
		tbGoods.setAuditStatus("0");
		goodsMapper.insert(tbGoods);// 插入商品基本信息

		goods.getGoodsDesc().setGoodsId(tbGoods.getId());// 将基本表goodsId存入详细表中
		goodsDescMapper.insert(goods.getGoodsDesc());

		saveItemList(goods);
	}

	private void setItemValue(TbItem item, Goods goods) {

		// 商品id
		item.setGoodsId(goods.getGoods().getId());
		// 商品分类,三级分类id
		item.setCategoryid(goods.getGoods().getCategory3Id());
		item.setCreateTime(new Date());// 创建日期
		item.setUpdateTime(new Date());// 更新日期
		// sellerId
		item.setSellerId(goods.getGoods().getSellerId());
		// category名
		item.setCategory(itemCatMapper.selectByPrimaryKey(item.getCategoryid()).getName());
		// brandName
		item.setBrand(brandMapper.selectByPrimaryKey(goods.getGoods().getBrandId()).getName());
		// 商家名称
		item.setSeller(sellerMapper.selectByPrimaryKey(goods.getGoods().getSellerId()).getNickName());
		// 第一个图片
		List<Map> itemImageList = JSON.parseArray(goods.getGoodsDesc().getItemImages(), Map.class);
		if (itemImageList.size() > 0) {
			item.setImage((String) itemImageList.get(0).get("url"));
		}

		itemMapper.insert(item);
	}

	/**
	 * 修改
	 */
	@Override
	public void update(Goods goods) {
		//更新基本表
		goodsMapper.updateByPrimaryKey(goods.getGoods());
		//更新扩展表
		goodsDescMapper.updateByPrimaryKey(goods.getGoodsDesc());
		//item表，先删除再添加
		
		TbItemExample example=new TbItemExample();
		com.pinyougou.pojo.TbItemExample.Criteria criteria = example.createCriteria();
		criteria.andGoodsIdEqualTo(goods.getGoods().getId());
		itemMapper.deleteByExample(example);
		
		//插入新的item
		saveItemList(goods);
	}

	/**
	 * 插入列表数据
	 */
	private void saveItemList(Goods goods) {
		if ("1".equals(goods.getGoods().getIsEnableSpec())) {
			// 插入items
			List<TbItem> itemList = goods.getItemList();
			if (itemList != null && itemList.size() > 0) {
				for (TbItem item : itemList) {
					// 构建标题
					// SPU名称+规格选项值
					String title = goods.getGoods().getGoodsName();
					Map<String, Object> map = JSON.parseObject(item.getSpec());
					for (String key : map.keySet()) {
						title += " " + map.get(key);
					}
					item.setTitle(title);
					setItemValue(item, goods);
				}
			}
		} else {
			// 不启用规格
			TbItem item = new TbItem();
			item.setTitle(goods.getGoods().getGoodsName());
			item.setPrice(goods.getGoods().getPrice());
			item.setNum(99999);
			item.setStatus("1");
			item.setIsDefault("1");
			setItemValue(item, goods);
		}
	}
	/**
	 * 根据ID获取实体
	 *
	 * @param id
	 * @return
	 */
	@Override
	public Goods findOne(Long id) {
		Goods goods = new Goods();
		goods.setGoods(goodsMapper.selectByPrimaryKey(id));
		goods.setGoodsDesc(goodsDescMapper.selectByPrimaryKey(id));
		
		TbItemExample example = new TbItemExample();
		com.pinyougou.pojo.TbItemExample.Criteria criteria = example.createCriteria();
		criteria.andGoodsIdEqualTo(id);
		goods.setItemList(itemMapper.selectByExample(example));
		return goods;
	}

	/**
	 * 批量删除
	 */
	@Override
	public void delete(Long[] ids) {
		for (Long id : ids) {
			TbGoods goods = goodsMapper.selectByPrimaryKey(id);
			goods.setIsDelete("1");
			goodsMapper.updateByPrimaryKey(goods);
		}
	}

	@Override
	public PageResult findPage(TbGoods goods, int pageNum, int pageSize) {
		PageHelper.startPage(pageNum, pageSize);

		TbGoodsExample example = new TbGoodsExample();
		Criteria criteria = example.createCriteria();
		//判断没有逻辑删除
		criteria.andIsDeleteIsNull();
		if (goods != null) {
			if (goods.getSellerId() != null && goods.getSellerId().length() > 0) {
//                criteria.andSellerIdLike("%" + goods.getSellerId() + "%");
				criteria.andSellerIdEqualTo(goods.getSellerId());
			}
			if (goods.getGoodsName() != null && goods.getGoodsName().length() > 0) {
				criteria.andGoodsNameLike("%" + goods.getGoodsName() + "%");
			}
			if (goods.getAuditStatus() != null && goods.getAuditStatus().length() > 0) {
				criteria.andAuditStatusLike("%" + goods.getAuditStatus() + "%");
			}
			if (goods.getIsMarketable() != null && goods.getIsMarketable().length() > 0) {
				criteria.andIsMarketableLike("%" + goods.getIsMarketable() + "%");
			}
			if (goods.getCaption() != null && goods.getCaption().length() > 0) {
				criteria.andCaptionLike("%" + goods.getCaption() + "%");
			}
			if (goods.getSmallPic() != null && goods.getSmallPic().length() > 0) {
				criteria.andSmallPicLike("%" + goods.getSmallPic() + "%");
			}
			if (goods.getIsEnableSpec() != null && goods.getIsEnableSpec().length() > 0) {
				criteria.andIsEnableSpecLike("%" + goods.getIsEnableSpec() + "%");
			}
			if (goods.getIsDelete() != null && goods.getIsDelete().length() > 0) {
				criteria.andIsDeleteLike("%" + goods.getIsDelete() + "%");
			}

		}

		Page<TbGoods> page = (Page<TbGoods>) goodsMapper.selectByExample(example);
		return new PageResult(page.getTotal(), page.getResult());
	}

	@Override
	public void updateStatus(Long[] ids, String auditStatus) {
		for (Long id : ids) {
			TbGoods goods = goodsMapper.selectByPrimaryKey(id);
			goods.setAuditStatus(auditStatus);
			goodsMapper.updateByPrimaryKey(goods);
		}
	}

}
