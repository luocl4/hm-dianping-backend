package com.hmdp.mq;

import com.hmdp.dto.SeckillOrderMessage;
import com.hmdp.entity.VoucherOrder;
import com.hmdp.service.ISeckillVoucherService;
import com.hmdp.service.IVoucherOrderService;
import com.hmdp.utils.RedisIdWorker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import static com.hmdp.constant.MqConstants.SECKILL_ORDER_QUEUE;

/**
 * 秒杀订单消息消费者 - 异步创建订单
 */
@Component
@Slf4j
public class SeckillOrderConsumer {

    @Autowired
    private IVoucherOrderService voucherOrderService;
    @Autowired
    private ISeckillVoucherService seckillVoucherService;
    @Autowired
    private RedisIdWorker redisIdWorker;

    /**
     * 监听秒杀订单队列，消费消息并创建订单
     */
    @RabbitListener(queues = SECKILL_ORDER_QUEUE)
    @Transactional
    public void handleSeckillOrder(SeckillOrderMessage message) {
        log.info("开始处理秒杀订单：{}", message);
        Long userId = message.getUserId();
        Long voucherId = message.getVoucherId();
        Long orderId = message.getOrderId();

        try {
            // 1.再次校验一人一单（防止Redis缓存穿透）
            int count = voucherOrderService.query()
                    .eq("user_id", userId)
                    .eq("voucher_id", voucherId)
                    .count();
            if (count > 0) {
                log.warn("用户{}已购买过秒杀券{}，取消重复订单{}", userId, voucherId, orderId);
                return;
            }

            // 2.扣减库存（最终校验，防止超卖）
            boolean success = seckillVoucherService.update()
                    .setSql("stock = stock - 1")
                    .eq("voucher_id", voucherId)
                    .gt("stock", 0)
                    .update();
            if (!success) {
                log.warn("秒杀券{}库存不足，订单{}创建失败", voucherId, orderId);
                // 可以在这里发送到死信队列或进行补偿处理
                return;
            }

            // 3.创建订单
            VoucherOrder voucherOrder = new VoucherOrder();
            voucherOrder.setId(orderId);
            voucherOrder.setUserId(userId);
            voucherOrder.setVoucherId(voucherId);
            voucherOrderService.save(voucherOrder);

            log.info("秒杀订单{}创建成功", orderId);
        } catch (Exception e) {
            log.error("秒杀订单{}创建失败", orderId, e);
            // 异常时消息会重新入队，可配置重试策略
            throw new RuntimeException("订单创建失败，触发重试", e);
        }
    }
}