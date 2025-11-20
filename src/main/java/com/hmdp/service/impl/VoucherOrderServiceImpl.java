package com.hmdp.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.Result;
import com.hmdp.entity.SeckillVoucher;
import com.hmdp.entity.VoucherOrder;
import com.hmdp.mapper.VoucherOrderMapper;
import com.hmdp.service.ISeckillVoucherService;
import com.hmdp.service.IVoucherOrderService;
import com.hmdp.utils.RedisIdWorker;
import com.hmdp.utils.UserHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class VoucherOrderServiceImpl extends ServiceImpl<VoucherOrderMapper, VoucherOrder> implements IVoucherOrderService {
    @Autowired
    private ISeckillVoucherService seckillVoucherService;
    @Autowired
    private VoucherOrderMapper voucherOrderMapper;
    @Autowired
    private RedisIdWorker redisIdWorker;

    /**
     * 秒杀券下单
     *
     * @param voucherId
     * @return
     */
    @Override
    @Transactional
    public Result seckillVoucher(Long voucherId) {
//        1.查询秒杀券信息
        SeckillVoucher voucher = seckillVoucherService.getById(voucherId);
//        2.判断秒杀是否开始,开始时间比现在时间晚则还没有开始
        if (voucher.getBeginTime().isAfter(LocalDateTime.now())) {
            return Result.fail("秒杀尚未开始");
        }
//        3.判断秒杀是否已经结束
        if (voucher.getEndTime().isBefore(LocalDateTime.now())) {
            return Result.fail("秒杀已经结束");
        }
//        4.判断库存是否充足，库存小于1，则不足
        if (voucher.getStock() < 1) {
            return Result.fail("库存不足");
        }
//        5.扣减库存，相当于 UPDATE seckill_voucher SET stock = stock - 1 WHERE voucher_id = #{voucherId};
//        用到mybatis-plus的https://baomidou.com/guides/data-interface/#%E4%BD%BF%E7%94%A8%E6%AD%A5%E9%AA%A4的update
        boolean success = seckillVoucherService.update().setSql("stock = stock - 1").eq("voucher_id", voucherId).update();
        if (!success) {
            return Result.fail("库存不足");
        }
//        6.创建订单
        VoucherOrder voucherOrder = new VoucherOrder();
//        6.1 订单id，用全局唯一id生成器
        long orderId = redisIdWorker.nextId("order");
        voucherOrder.setId(orderId);
//        6.2 用户id
        voucherOrder.setUserId(UserHolder.getUser().getId());
//        6.3 代金券id
        voucherOrder.setVoucherId(voucherId);
//        6.4 把订单写入数据库
        save(voucherOrder);
//        7,返回订单id
        return Result.ok(orderId);
    }
}
