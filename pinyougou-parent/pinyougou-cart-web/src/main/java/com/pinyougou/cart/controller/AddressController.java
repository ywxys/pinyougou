package com.pinyougou.cart.controller;

import java.util.List;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.alibaba.dubbo.config.annotation.Reference;
import com.pinyougou.pojo.TbAddress;
import com.pinyougou.user.service.AddressService;

import entity.PageResult;
import entity.Result;

/**
 * controller
 *
 * @author Administrator
 */
@RestController
@RequestMapping("/address")
public class AddressController {

    @Reference
    private AddressService addressService;

    /**
     * 返回全部列表
     *
     * @return
     */
    @RequestMapping("/findAll")
    public List<TbAddress> findAll() {
        return addressService.findAll();
    }


    /**
     * 返回全部列表
     *
     * @return
     */
    @RequestMapping("/findPage")
    public PageResult findPage(int page, int rows) {
        return addressService.findPage(page, rows);
    }

    /**
     * 增加
     *
     * @param address
     * @return
     */
    @RequestMapping("/add")
    public Result add(@RequestBody TbAddress address) {
        //设置收货地址的userId
        String name = SecurityContextHolder.getContext().getAuthentication().getName();
        address.setUserId(name);
        try {
            addressService.add(address);
            return new Result(true, "增加成功");
        } catch (Exception e) {
            e.printStackTrace();
            return new Result(false, "增加失败");
        }
    }

    /**
     * 修改
     *
     * @param address
     * @return
     */
    @RequestMapping("/update")
    public Result update(@RequestBody TbAddress address) {
        String name = SecurityContextHolder.getContext().getAuthentication().getName();
        if (!name.equals(address.getUserId())) {//不是当前用户的
            return new Result(false, "非法操作");
        }
        try {
            addressService.update(address);
            return new Result(true, "修改成功");
        } catch (Exception e) {
            e.printStackTrace();
            return new Result(false, "修改失败");
        }
    }

    /**
     * 获取实体
     *
     * @param id
     * @return
     */
    @RequestMapping("/findOne")
    public TbAddress findOne(Long id) {
        String name = SecurityContextHolder.getContext().getAuthentication().getName();
        TbAddress address = addressService.findOne(id);
        if (!name.equals(address.getUserId())) {//非法操作,查询的不是当前用户的地址
            return null;
        }
        return address;
    }

    /**
     * 批量删除
     *
     * @param id
     * @return
     */
    @RequestMapping("/delete")
    public Result delete(Long id) {
        Long[] ids = {id};
        try {
            addressService.delete(ids);
            return new Result(true, "删除成功");
        } catch (Exception e) {
            e.printStackTrace();
            return new Result(false, "删除失败");
        }
    }

    /**
     * 查询+分页
     *
     * @param address
     * @param page
     * @param rows
     * @return
     */
    @RequestMapping("/search")
    public PageResult search(@RequestBody TbAddress address, int page, int rows) {
        return addressService.findPage(address, page, rows);
    }

    /**
     * 查询用户收件地址列表
     *
     * @return
     */
    @RequestMapping("/findListByUserId")
    public List<TbAddress> findListByUserId() {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        return addressService.findListByUserId(userId);
    }
}
