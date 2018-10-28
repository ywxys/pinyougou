package com.pinyougou.search.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.pinyougou.pojo.TbItem;
import com.pinyougou.search.service.ItemSearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.TextMessage;
import java.util.List;
import java.util.Map;

@Component
public class ItemSearchListener implements MessageListener {
    @Autowired
    private ItemSearchService itemSearchService;


    @Override
    public void onMessage(Message message) {
        System.out.println("接收到消息。。。");
        try {
            TextMessage textMessage = (TextMessage) message;
            //获取json字符串对象
            String text = textMessage.getText();
            //解析为list集合
            List<TbItem> itemList = JSON.parseArray(text, TbItem.class);
            //为列表中item对象加specMap
            for (TbItem item : itemList) {
                System.out.println(item.getId()+"---"+item.getTitle());
                String specStr = item.getSpec();
                Map specMap = JSON.parseObject(specStr);
                item.setSpecMap(specMap);//给动态域specMap赋值
            }
            //导入solr索引库
            itemSearchService.importList(itemList);
            System.out.println("成功导入索引库");
        } catch (JMSException e) {
            e.printStackTrace();
        }
    }
}
