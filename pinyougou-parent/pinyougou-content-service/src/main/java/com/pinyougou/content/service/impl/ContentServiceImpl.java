package com.pinyougou.content.service.impl;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import com.alibaba.dubbo.config.annotation.Service;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.pinyougou.mapper.TbContentMapper;
import com.pinyougou.pojo.TbContent;
import com.pinyougou.pojo.TbContentExample;
import com.pinyougou.pojo.TbContentExample.Criteria;
import com.pinyougou.content.service.ContentService;

import entity.PageResult;
import org.springframework.data.redis.core.RedisTemplate;

/**
 * 服务实现层
 *
 * @author Administrator
 */
@Service
public class ContentServiceImpl implements ContentService {

	@Autowired
	private TbContentMapper contentMapper;

	/**
	 * 查询全部
	 */
	@Override
	public List<TbContent> findAll() {
		return contentMapper.selectByExample(null);
	}

	/**
	 * 按分页查询
	 */
	@Override
	public PageResult findPage(int pageNum, int pageSize) {
		PageHelper.startPage(pageNum, pageSize);
		Page<TbContent> page = (Page<TbContent>) contentMapper.selectByExample(null);
		return new PageResult(page.getTotal(), page.getResult());
	}

	/**
	 * 增加
	 */
	@Override
	public void add(TbContent content) {
		contentMapper.insert(content);
		removeRedis(content.getCategoryId());
	}

	/**
	 * 修改
	 */
	@Override
	public void update(TbContent content) {
		// 删除之前组缓存
		removeRedis(contentMapper.selectByPrimaryKey(content.getId()).getCategoryId());
		// 删除更新后组缓存
		removeRedis(content.getCategoryId());
		contentMapper.updateByPrimaryKey(content);
	}

	/**
	 * 根据ID获取实体
	 *
	 * @param id
	 * @return
	 */
	@Override
	public TbContent findOne(Long id) {
		return contentMapper.selectByPrimaryKey(id);
	}

	/**
	 * 批量删除
	 */
	@Override
	public void delete(Long[] ids) {
		for (Long id : ids) {
			// 删除前清除缓存
			removeRedis(contentMapper.selectByPrimaryKey(id).getCategoryId());

			contentMapper.deleteByPrimaryKey(id);
		}
	}

	@Override
	public PageResult findPage(TbContent content, int pageNum, int pageSize) {
		PageHelper.startPage(pageNum, pageSize);

		TbContentExample example = new TbContentExample();
		Criteria criteria = example.createCriteria();

		if (content != null) {
			if (content.getTitle() != null && content.getTitle().length() > 0) {
				criteria.andTitleLike("%" + content.getTitle() + "%");
			}
			if (content.getUrl() != null && content.getUrl().length() > 0) {
				criteria.andUrlLike("%" + content.getUrl() + "%");
			}
			if (content.getPic() != null && content.getPic().length() > 0) {
				criteria.andPicLike("%" + content.getPic() + "%");
			}
			if (content.getStatus() != null && content.getStatus().length() > 0) {
				criteria.andStatusLike("%" + content.getStatus() + "%");
			}

		}

		Page<TbContent> page = (Page<TbContent>) contentMapper.selectByExample(example);
		return new PageResult(page.getTotal(), page.getResult());
	}

	@Autowired
	private RedisTemplate redisTemplate;

	@Override
	public List<TbContent> findByCategoryId(Long categoryId) {
		// 查询缓存不能影响正常逻辑
		try {
			// 先查缓存
			List<TbContent> list = (List<TbContent>) redisTemplate.boundHashOps("content").get(categoryId);
			if (list != null && list.size() > 0) {
				return list;
			}
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("查询缓存出错");
			// 后续解决
		}
		// 缓存中没有数据
		TbContentExample example = new TbContentExample();
		Criteria criteria = example.createCriteria();
		criteria.andCategoryIdEqualTo(categoryId);// 根据categoryId查询
		criteria.andStatusEqualTo("1");// 启用
		example.setOrderByClause("sort_order");// 排序
		List<TbContent> list = contentMapper.selectByExample(example);

		try {
			redisTemplate.boundHashOps("content").put(categoryId, list);
			System.out.println("存数据到redis中");
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("存数据到redis时出现异常了！");
		}
		return list;
	}

	/**
	 * 清除缓存
	 */
	public void removeRedis(Long categoryId) {
		redisTemplate.boundHashOps("content").delete(categoryId);
	}
}
