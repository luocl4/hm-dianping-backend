package com.hmdp.utils;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest  // 启动 Spring 上下文，自动注入依赖

class RedisIdWorkerTest {
    @Autowired
    private RedisIdWorker redisIdWorker;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    // 测试单线程下 ID 唯一性、序列号自增、Redis 键生成
    @Test
    public void testNextId_SingleThread() {
        // 1. 定义测试参数
        String keyPrefix = "test_single";  // 业务前缀
        int testCount = 1000;              // 生成 1000 个 ID
        Set<Long> idSet = new HashSet<>(); // 存储 ID，验证唯一性（Set 自动去重）

        // 2. 生成 ID 并验证
        for (int i = 0; i < testCount; i++) {
            long id = redisIdWorker.nextId(keyPrefix);
            System.out.printf("第 %d 个 ID：%d%n", i + 1, id);

            // 验证 1：ID 不为 0（避免生成失败）
            assertNotEquals(0, id, "ID 生成失败，结果为 0");

            // 验证 2：ID 唯一（Set 中不存在重复）
            boolean isAdded = idSet.add(id);
            assertTrue(isAdded, "出现重复 ID：" + id);
        }

        // 3. 验证 Redis 键和序列号（当前日期的键，值应为 1000）
        String date = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));
        String redisKey = "icr:" + keyPrefix + ":" + date;
        String sequenceInRedis = stringRedisTemplate.opsForValue().get(redisKey);

        // 验证 3：Redis 中存在对应的计数器键
        assertNotNull(sequenceInRedis, "Redis 中未生成计数器键：" + redisKey);

        // 验证 4：序列号最终值等于生成的 ID 数量（1000）
        assertEquals(testCount, Long.parseLong(sequenceInRedis), "Redis 序列号与生成数量不一致");

        // 清理测试数据（可选，避免污染 Redis）
        stringRedisTemplate.delete(redisKey);
        System.out.println("单线程测试通过！");
    }
}