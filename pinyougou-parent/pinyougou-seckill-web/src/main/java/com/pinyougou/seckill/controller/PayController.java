package com.pinyougou.seckill.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.pinyougou.pay.service.WeixinPayService;
import com.pinyougou.pojo.TbSeckillOrder;
import com.pinyougou.seckill.service.SeckillOrderService;
import entity.Result;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("pay")
public class PayController {
    @Reference(timeout = 10000)
    private SeckillOrderService seckillOrderService;

    @Reference
    private WeixinPayService weixinPayService;

    @RequestMapping("createNative")
    public Map createNative(){
        TbSeckillOrder seckillOrder = seckillOrderService.findOrderFromRedisByUserId(SecurityContextHolder.getContext().getAuthentication().getName());
        if (seckillOrder != null) {
            return weixinPayService.createNative(seckillOrder.getId() + "", seckillOrder.getMoney().doubleValue() * 100 + "");
        } else {
            return new HashMap();
        }
    }

    @RequestMapping("/queryPayStatus")
    public Result queryPayStatus(String out_trade_no) {
        //获取当前用户
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        Result result = null;
        int x = 0;
        while (true) {
            Map map = weixinPayService.queryPayStatus(out_trade_no);
            if (map == null) {
                //查询出错
                result = new Result(false, "支付失败");
                break;
            }
            if("SUCCESS".equals(map.get("trade_state"))){
                //支付成功
                result = new Result(true, "支付成功");
                //存到数据库
                seckillOrderService.saveOrderFromRedisToDb(userId, Long.valueOf(out_trade_no), map.get("transaction_id")+"");
                break;
            }
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            x++;//5分钟超时
            if (x > 100) {
                result = new Result(false, "二维码超时");
                //取消订单
                seckillOrderService.payFail(userId,Long.parseLong(out_trade_no));
                Map payresult = weixinPayService.closePay(out_trade_no);
                if ("SUCCESS".equals(payresult.get("return_code"))) {
                    //正常执行
                    if ("ORDERPAID".equals(payresult.get("err_code"))) {
                        result=new Result(true, "支付成功");
                        seckillOrderService.saveOrderFromRedisToDb(userId, Long.valueOf(out_trade_no), map.get("transaction_id")+"");
                    }
                }
                if (!result.isSuccess()) {
                    System.out.println("二维码超时");
                    //支付失败
                    seckillOrderService.payFail(userId, Long.valueOf(out_trade_no));
                }
                break;
            }
        }

        return result;
    }
}
