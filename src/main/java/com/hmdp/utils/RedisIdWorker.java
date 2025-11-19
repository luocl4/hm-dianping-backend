package com.hmdp.utils;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Component
public class RedisIdWorker {
    // 自定义起始时间戳：2025-01-01 00:00:00 UTC；如果不自定义直接用的话，数值会很大，然后转成二进制就会超出31位
    private static final long START_TIMESTAMP = 1735689600000L;
    //    左移的位数，32位
    private static final int SEQUENCE_BITS = 32;

    private StringRedisTemplate stringRedisTemplate;

    public RedisIdWorker(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;  //@Component 注解的东西里面要自己写构造函数，才能用StringRedisTemplate
    }

    /**
     * 生成全局唯一id的函数
     *
     * @param keyPrefix
     * @return
     */
    public long nextId(String keyPrefix) {
//        1.生成时间戳
        long timestamp = System.currentTimeMillis();  //这个函数得到的是，从1970 年 1 月 1 日 00:00:00 UTC开始的ms数
//        2.生成序列号（Redis自增，原子操作，避免并发冲突）
//        Redis key：比如 "order:id:20251119"（按日期拆分，方便后续清理）；一天一个新的key
        String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String redisKey = "icr:" + keyPrefix + ":" + date;
        long sequence = stringRedisTemplate.opsForValue().increment(redisKey);  // 等价于 INCRBY key 1，即 INCR key
//        3.拼接并返回
        return (timestamp - START_TIMESTAMP) << SEQUENCE_BITS | sequence;
    }
}
