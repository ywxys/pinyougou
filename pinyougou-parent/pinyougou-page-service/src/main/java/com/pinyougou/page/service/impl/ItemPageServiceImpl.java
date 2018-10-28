package com.pinyougou.page.service.impl;

import com.pinyougou.mapper.TbGoodsDescMapper;
import com.pinyougou.mapper.TbGoodsMapper;
import com.pinyougou.mapper.TbItemCatMapper;
import com.pinyougou.mapper.TbItemMapper;
import com.pinyougou.page.service.ItemPageService;
import com.pinyougou.pojo.TbGoods;
import com.pinyougou.pojo.TbGoodsDesc;
import com.pinyougou.pojo.TbItem;
import com.pinyougou.pojo.TbItemExample;
import com.pinyougou.pojo.TbItemExample.Criteria;

import freemarker.template.Configuration;
import freemarker.template.Template;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.view.freemarker.FreeMarkerConfig;

import java.io.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
@Service
public class ItemPageServiceImpl implements ItemPageService {
    @Value("${pagedir}")
    private String pagedir;

    @Autowired
    private FreeMarkerConfig freeMarkerConfig;

    @Autowired
    private TbGoodsMapper goodsMapper;

    @Autowired
    private TbGoodsDescMapper goodsDescMapper;

    @Autowired
    private TbItemMapper itemMapper;

    @Autowired
    private TbItemCatMapper itemCatMapper;


    @Override
    public boolean genItemHtml(Long goodsId) {
        Writer out=null;
        try {
            //获取配置对象
            Configuration configuration = freeMarkerConfig.getConfiguration();
            //获取模板文件
            Template template = configuration.getTemplate("item.ftl");
            //获取文件写出流对象
            out = new BufferedWriter(new FileWriter(new File(pagedir + goodsId + ".html")));
            //获取商品详情
            //商品信息
            Map dataModel = new HashMap();
            TbGoods goods = goodsMapper.selectByPrimaryKey(goodsId);
            dataModel.put("goods", goods);
            //商品详细信息
            TbGoodsDesc goodsDesc=goodsDescMapper.selectByPrimaryKey(goodsId);
            dataModel.put("goodsDesc", goodsDesc);
            //商品分类信息
            String itemCat1 = itemCatMapper.selectByPrimaryKey(goods.getCategory1Id()).getName();
            dataModel.put("itemCat1", itemCat1);
            String itemCat2 = itemCatMapper.selectByPrimaryKey(goods.getCategory2Id()).getName();
            dataModel.put("itemCat2", itemCat2);
            String itemCat3 = itemCatMapper.selectByPrimaryKey(goods.getCategory3Id()).getName();
            dataModel.put("itemCat3", itemCat3);
            //sku列表
            TbItemExample example = new TbItemExample();
            Criteria criteria = example.createCriteria();
            criteria.andGoodsIdEqualTo(goodsId);//指定goodsId
            criteria.andStatusEqualTo("1");//状态为有效
            example.setOrderByClause("is_default desc");//按是否默认降序
            List<TbItem> itemList = itemMapper.selectByExample(example);
            dataModel.put("itemList", itemList);

            //生成模板文件
            template.process(dataModel,out);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            //关闭流
            try {
                out.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public boolean deleteItemHtml(Long[] goodsIds) {
        try {
            for (Long goodsId : goodsIds) {
                new File(pagedir + goodsId+".html").delete();
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
