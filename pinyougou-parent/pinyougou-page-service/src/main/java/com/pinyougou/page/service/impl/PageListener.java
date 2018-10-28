package com.pinyougou.page.service.impl;

import com.pinyougou.page.service.ItemPageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.TextMessage;

@Component
public class PageListener implements MessageListener {

    @Autowired
    private ItemPageService itemPageService;

    @Override
    public void onMessage(Message message) {
        try {
            //接收到生成静态页面消息
            TextMessage textMessage = (TextMessage) message;
            String text = textMessage.getText();
            System.out.println("接收到goodsId："+text);
            //创建静态页面
            boolean b = itemPageService.genItemHtml(Long.parseLong(text));
        } catch (JMSException e) {
            e.printStackTrace();
        }
    }
}
