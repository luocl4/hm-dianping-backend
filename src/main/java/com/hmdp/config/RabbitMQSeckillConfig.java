package com.hmdp.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static com.hmdp.constant.MqConstants.*;

/**
 * RabbitMQ配置类 - 秒杀订单队列
 */
@Configuration
public class RabbitMQSeckillConfig {

    /**
     * 声明队列 - 持久化、非独占、不自动删除
     */
    @Bean
    public Queue seckillOrderQueue() {
        return QueueBuilder.durable(SECKILL_ORDER_QUEUE)
                .build();
    }

    /**
     * 声明交换机 - direct类型（精准路由）
     */
    @Bean
    public DirectExchange seckillOrderExchange() {
        return ExchangeBuilder.directExchange(SECKILL_ORDER_EXCHANGE)
                .durable(true)
                .build();
    }

    /**
     * 绑定队列和交换机
     */
    @Bean
    public Binding seckillOrderBinding(Queue seckillOrderQueue, DirectExchange seckillOrderExchange) {
        return BindingBuilder.bind(seckillOrderQueue)
                .to(seckillOrderExchange)
                .with(SECKILL_ORDER_ROUTING_KEY);
    }

}