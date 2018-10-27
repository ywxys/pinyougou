package com.pinyougou.solrutil;

import com.alibaba.fastjson.JSON;
import com.pinyougou.mapper.TbGoodsMapper;
import com.pinyougou.mapper.TbItemMapper;
import com.pinyougou.pojo.TbGoods;
import com.pinyougou.pojo.TbGoodsExample;
import com.pinyougou.pojo.TbGoodsExample.Criteria;
import com.pinyougou.pojo.TbItem;
import com.pinyougou.pojo.TbItemExample;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.data.solr.core.SolrTemplate;
import org.springframework.data.solr.core.query.SimpleQuery;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class SolrUtil {
    @Autowired
    private TbItemMapper itemMapper;
    
    @Autowired
    private TbGoodsMapper goodsMapper;

    @Autowired
    private SolrTemplate solrTemplate;

    public void importItemData() {
    	solrTemplate.delete(new SimpleQuery("*:*"));
    	//商品已审核
    	TbGoodsExample goodsExample = new TbGoodsExample();
    	Criteria goodsCriteria = goodsExample.createCriteria();
    	goodsCriteria.andAuditStatusEqualTo("1");
		List<TbGoods> goodsList = goodsMapper.selectByExample(goodsExample);
		List<Long> goodsId =new ArrayList<>();
		for (TbGoods goods : goodsList) {
			goodsId.add(goods.getId());
		}
		//sku开启
        TbItemExample example = new TbItemExample();
        TbItemExample.Criteria criteria = example.createCriteria();
        criteria.andStatusEqualTo("1");//审核通过
        criteria.andGoodsIdIn(goodsId);
        List<TbItem> itemList = itemMapper.selectByExample(example);
        System.out.println("---------商品列表---------");
        for (TbItem item : itemList) {
            System.out.println(item.getId()+" "+item.getTitle());
            Map specMap = JSON.parseObject(item.getSpec(), Map.class);//提取json字符串转为map
            item.setSpecMap(specMap);
        }
        System.out.println("------------结束----------");
        solrTemplate.saveBeans(itemList);
        solrTemplate.commit();
    }

    public static void main(String[] args) {
        ApplicationContext ac = new ClassPathXmlApplicationContext("classpath*:spring/applicationContext*.xml");
        SolrUtil solrUtil = ac.getBean("solrUtil",SolrUtil.class);
        solrUtil.importItemData();
    }
}
