package com.hmdp.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.Result;
import com.hmdp.dto.SeckillOrderMessage;
import com.hmdp.entity.SeckillVoucher;
import com.hmdp.entity.VoucherOrder;
import com.hmdp.mapper.VoucherOrderMapper;
import com.hmdp.service.ISeckillVoucherService;
import com.hmdp.service.IVoucherOrderService;
import com.hmdp.utils.RedisIdWorker;
import com.hmdp.utils.UserHolder;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.aop.framework.AopContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;

import static com.hmdp.constant.MqConstants.SECKILL_ORDER_EXCHANGE;
import static com.hmdp.constant.MqConstants.SECKILL_ORDER_ROUTING_KEY;

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

    // Lua 脚本：校验秒杀资格（开始时间、结束时间、库存、一人一单）
    private static final DefaultRedisScript<Long> SECKILL_SCRIPT;

    static {
        SECKILL_SCRIPT = new DefaultRedisScript<>();
        SECKILL_SCRIPT.setLocation(new ClassPathResource("lua/seckill.lua"));
        SECKILL_SCRIPT.setResultType(Long.class);
    }

    @Autowired
    private ISeckillVoucherService seckillVoucherService;
    @Autowired
    private VoucherOrderMapper voucherOrderMapper;
    @Autowired
    private RedisIdWorker redisIdWorker;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    // 注入RabbitTemplate用于发送消息
    @Autowired
    private RabbitTemplate rabbitTemplate;

    /**
     * 秒杀券下单
     *
     * @param voucherId
     * @return
     */
    @Override
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
        Long userId = UserHolder.getUser().getId();
        synchronized (userId.toString().intern()) {  //悲观锁
            IVoucherOrderService proxy = (IVoucherOrderService) AopContext.currentProxy();  //获取事务代理对象
            return proxy.createVoucherOrder(voucherId); //用事务代理对象调用，才可以用 @Transactional，不然会事务失效
        }
    }

    /**
     * 真正创建订单的函数
     *
     * @param voucherId
     * @return
     */
    @Transactional
    public Result createVoucherOrder(Long voucherId) {
        Long userId = UserHolder.getUser().getId();
//        5.一人一单
//        5.1 查询订单,等价于 SELECT COUNT(*) FROM tb_voucher_order WHERE user_id = #{userId} AND voucher_id = #{voucherId};
        int count = query().eq("user_id", userId).eq("voucher_id", voucherId).count();
//        5.2 判断是否存在
        if (count > 0) {
            return Result.fail("该用户已经购买过了");
        }
//        6.扣减库存，相当于
//        用到mybatis-plus的https://baomidou.com/guides/data-interface/#%E4%BD%BF%E7%94%A8%E6%AD%A5%E9%AA%A4的update
//        普通的乐观锁，是 UPDATE tb_seckill_voucher SET stock = stock - 1 WHERE voucher_id = #{voucherId} and stock = #{voucher.getStock()}；
//        这样会让成功率非常低，在我们这里可以改成 UPDATE tb_seckill_voucher SET stock = stock - 1 WHERE voucher_id = #{voucherId} and stock > 0;
//        不强制要求相等，而是有就可以买
        boolean success = seckillVoucherService.update().setSql("stock = stock - 1").eq("voucher_id", voucherId)
                .gt("stock", 0).update();
        if (!success) {
            return Result.fail("库存不足");
        }
//        7.创建订单
        VoucherOrder voucherOrder = new VoucherOrder();
//        7.1 订单id，用全局唯一id生成器
        long orderId = redisIdWorker.nextId("order");
        voucherOrder.setId(orderId);
//        7.2 用户id
        voucherOrder.setUserId(UserHolder.getUser().getId());
//        7.3 代金券id
        voucherOrder.setVoucherId(voucherId);
//        7.4 把订单写入数据库
        save(voucherOrder);
//        8,返回订单id
        return Result.ok(orderId);

    }

    /**
     * 秒杀券下单,异步优化写法
     *
     * @param voucherId
     * @return
     */
    @Override
    public Result seckillVoucherAsync(Long voucherId) {
//        1.查询秒杀券信息
        SeckillVoucher voucher = seckillVoucherService.getById(voucherId);
//        判断秒杀是否开始,开始时间比现在时间晚则还没有开始
        if (voucher.getBeginTime().isAfter(LocalDateTime.now())) {
            return Result.fail("秒杀尚未开始");
        }
//        判断秒杀是否已经结束
        if (voucher.getEndTime().isBefore(LocalDateTime.now())) {
            return Result.fail("秒杀已经结束");
        }
//        2.执行lua脚本
        Long userId = UserHolder.getUser().getId();
        Long result = stringRedisTemplate.execute(
                SECKILL_SCRIPT,
                Collections.emptyList(),
                voucherId.toString(),
                userId.toString()
        );
//        3.判断结果是为0
        int r = result.intValue();
//        3.1.不为0,代表没有购买资格
        if (r != 0) {
            return Result.fail(r == 1 ? "秒杀券库存不足" : "该用户已经购买过该秒杀券");
        }
//        3.2.为0,有购买资格,把下单信息保存到阻塞队列
        // 生成订单ID，创建消息对象
        long orderId = redisIdWorker.nextId("order");
        SeckillOrderMessage orderMessage = new SeckillOrderMessage(orderId, userId, voucherId); //全参构造
        // 发送消息到RabbitMQ队列（异步创建订单），direct类型
        rabbitTemplate.convertAndSend(
                SECKILL_ORDER_EXCHANGE,
                SECKILL_ORDER_ROUTING_KEY,
                orderMessage
        );
//        4.返回订单id
        return Result.ok(orderId);
    }


}
