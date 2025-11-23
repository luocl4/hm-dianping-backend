package com.hmdp.controller;


import com.hmdp.dto.Result;
import com.hmdp.service.IVoucherOrderService;
import io.swagger.annotations.Api;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * <p>
 * 前端控制器
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@RestController
@RequestMapping("/voucher-order")
@Api(tags = "秒杀券下单")
public class VoucherOrderController {
    @Autowired
    private IVoucherOrderService voucherOrderService;


    /**
     * 秒杀券下单
     *
     * @param voucherId
     * @return
     */
    @PostMapping("seckill/{id}")
    public Result seckillVoucher(@PathVariable("id") Long voucherId) {
        //普通的写法，包括超卖和一人一单问题，乐观锁和悲观锁
//        return voucherOrderService.seckillVoucher(voucherId);
        //异步优化秒杀
        return voucherOrderService.seckillVoucherAsync(voucherId);
    }
}
