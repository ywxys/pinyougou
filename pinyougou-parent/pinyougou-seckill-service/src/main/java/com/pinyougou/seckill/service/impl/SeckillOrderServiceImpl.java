package com.pinyougou.seckill.service.impl;

import java.util.Date;
import java.util.List;

import com.pinyougou.mapper.TbAddressMapper;
import com.pinyougou.mapper.TbSeckillGoodsMapper;
import com.pinyougou.pojo.TbSeckillGoods;
import com.pinyougou.seckill.service.SeckillOrderService;
import org.springframework.beans.factory.annotation.Autowired;
import com.alibaba.dubbo.config.annotation.Service;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.pinyougou.mapper.TbSeckillOrderMapper;
import com.pinyougou.pojo.TbSeckillOrder;
import com.pinyougou.pojo.TbSeckillOrderExample;
import com.pinyougou.pojo.TbSeckillOrderExample.Criteria;

import entity.PageResult;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import util.IdWorker;

/**
 * 服务实现层
 *
 * @author Administrator
 */
@Service
public class SeckillOrderServiceImpl implements SeckillOrderService {

    @Autowired
    private TbSeckillOrderMapper seckillOrderMapper;
    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private TbSeckillGoodsMapper seckillGoodsMapper;
    @Autowired
    private IdWorker idWorker;

    /**
     * 查询全部
     */
    @Override
    public List<TbSeckillOrder> findAll() {
        return seckillOrderMapper.selectByExample(null);
    }

    /**
     * 按分页查询
     */
    @Override
    public PageResult findPage(int pageNum, int pageSize) {
        PageHelper.startPage(pageNum, pageSize);
        Page<TbSeckillOrder> page = (Page<TbSeckillOrder>) seckillOrderMapper.selectByExample(null);
        return new PageResult(page.getTotal(), page.getResult());
    }

    /**
     * 增加
     */
    @Override
    public void add(TbSeckillOrder seckillOrder) {
        seckillOrderMapper.insert(seckillOrder);
    }


    /**
     * 修改
     */
    @Override
    public void update(TbSeckillOrder seckillOrder) {
        seckillOrderMapper.updateByPrimaryKey(seckillOrder);
    }

    /**
     * 根据ID获取实体
     *
     * @param id
     * @return
     */
    @Override
    public TbSeckillOrder findOne(Long id) {
        return seckillOrderMapper.selectByPrimaryKey(id);
    }

    /**
     * 批量删除
     */
    @Override
    public void delete(Long[] ids) {
        for (Long id : ids) {
            seckillOrderMapper.deleteByPrimaryKey(id);
        }
    }


    @Override
    public PageResult findPage(TbSeckillOrder seckillOrder, int pageNum, int pageSize) {
        PageHelper.startPage(pageNum, pageSize);

        TbSeckillOrderExample example = new TbSeckillOrderExample();
        Criteria criteria = example.createCriteria();

        if (seckillOrder != null) {
            if (seckillOrder.getUserId() != null && seckillOrder.getUserId().length() > 0) {
                criteria.andUserIdLike("%" + seckillOrder.getUserId() + "%");
            }
            if (seckillOrder.getSellerId() != null && seckillOrder.getSellerId().length() > 0) {
                criteria.andSellerIdEqualTo(seckillOrder.getSellerId());
            }
            if (seckillOrder.getStatus() != null && seckillOrder.getStatus().length() > 0) {
                criteria.andStatusLike("%" + seckillOrder.getStatus() + "%");
            }
            if (seckillOrder.getReceiverAddress() != null && seckillOrder.getReceiverAddress().length() > 0) {
                criteria.andReceiverAddressLike("%" + seckillOrder.getReceiverAddress() + "%");
            }
            if (seckillOrder.getReceiverMobile() != null && seckillOrder.getReceiverMobile().length() > 0) {
                criteria.andReceiverMobileLike("%" + seckillOrder.getReceiverMobile() + "%");
            }
            if (seckillOrder.getReceiver() != null && seckillOrder.getReceiver().length() > 0) {
                criteria.andReceiverLike("%" + seckillOrder.getReceiver() + "%");
            }
            if (seckillOrder.getTransactionId() != null && seckillOrder.getTransactionId().length() > 0) {
                criteria.andTransactionIdLike("%" + seckillOrder.getTransactionId() + "%");
            }

        }

        Page<TbSeckillOrder> page = (Page<TbSeckillOrder>) seckillOrderMapper.selectByExample(example);
        return new PageResult(page.getTotal(), page.getResult());
    }

    @Override
    public void submitOrder(Long id, String userId) {
        TbSeckillGoods seckillGoods = null;
        synchronized (SeckillGoodsServiceImpl.class) {//线程同步,从获取到减库存再到放回redis防止线程冲突
            //从缓存中查询秒杀商品
            seckillGoods = (TbSeckillGoods) redisTemplate.boundHashOps("seckillGoods").get(id);
            if (seckillGoods == null) {//非空判断
                throw new RuntimeException("商品不存在");
            }
            if (seckillGoods.getStockCount() <= 0) {
                throw new RuntimeException("商品已被抢空");
            }
            //减库存
            seckillGoods.setStockCount(seckillGoods.getStockCount() - 1);
            //放回缓存
            redisTemplate.boundHashOps("seckillGoods").put(id, seckillGoods);
        }
        /*if (seckillGoods.getStockCount() == 0) {
        不能删，不好恢复
            //抢光了，同步到数据库，从缓存中删除
            seckillGoodsMapper.updateByPrimaryKey(seckillGoods);
            redisTemplate.boundHashOps("seckillGoods").delete(id);
        }*/
        //保存订单到redis中
        long orderId = idWorker.nextId();
        TbSeckillOrder seckillOrder = new TbSeckillOrder();
        seckillOrder.setId(orderId);
        seckillOrder.setCreateTime(new Date());
        seckillOrder.setSeckillId(seckillGoods.getId());
        seckillOrder.setMoney(seckillGoods.getCostPrice());
        seckillOrder.setSellerId(seckillGoods.getSellerId());
        seckillOrder.setUserId(userId);//设置用户ID
        seckillOrder.setStatus("0");//状态
        redisTemplate.boundHashOps("seckillOrder").put(userId, seckillOrder);
    }

    @Override
    public TbSeckillOrder findOrderFromRedisByUserId(String userId) {
        return (TbSeckillOrder) redisTemplate.boundHashOps("seckillOrder").get(userId);
    }

    @Override
    public void saveOrderFromRedisToDb(String userId, Long orderId, String transactionId) {
        TbSeckillOrder seckillOrder = (TbSeckillOrder) redisTemplate.boundHashOps("seckillOrder").get(userId);
        if (seckillOrder == null) {
            throw new RuntimeException("订单不存在");
        }
        if (orderId.longValue() != seckillOrder.getId().longValue()) {
            throw new RuntimeException("订单不符");
        }
        seckillOrder.setTransactionId(transactionId);
        seckillOrder.setPayTime(new Date());
        seckillOrder.setStatus("1");
        seckillOrderMapper.insert(seckillOrder);//插入数据库
        redisTemplate.boundHashOps("seckillOrder").delete(userId);
    }

    @Override
    public void payFail(String userId, Long orderId) {
        TbSeckillOrder seckillOrder = (TbSeckillOrder) redisTemplate.boundHashOps("seckillOrder").get(userId);
        if (seckillOrder == null) {
            throw new RuntimeException("订单不存在");
        }
        if (seckillOrder.getId().longValue() != orderId.longValue()) {
            throw new RuntimeException("订单不符");
        }
        redisTemplate.boundHashOps("seckillOrder").delete(userId);//删除订单缓存
        //恢复库存
        synchronized (SeckillOrderServiceImpl.class) {
            TbSeckillGoods seckillGoods = (TbSeckillGoods) redisTemplate.boundHashOps("seckillGoods").get(seckillOrder.getSeckillId());
            if (seckillGoods != null) {
                seckillGoods.setStockCount(seckillGoods.getStockCount() + 1);//库存+1
                redisTemplate.boundHashOps("seckillGoods").put(seckillGoods.getId(), seckillGoods);//放入缓存
            }
        }

    }
}
