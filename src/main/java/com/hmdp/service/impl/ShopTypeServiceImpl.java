package com.hmdp.service.impl;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.Result;
import com.hmdp.entity.ShopType;
import com.hmdp.mapper.ShopTypeMapper;
import com.hmdp.service.IShopTypeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.hmdp.constant.RedisConstants.CACHE_SHOP_TYPE_KEY;
import static com.hmdp.constant.RedisConstants.CACHE_SHOP_TYPE_TTL;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class ShopTypeServiceImpl extends ServiceImpl<ShopTypeMapper, ShopType> implements IShopTypeService {
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 店铺类型查询业务（优化缓存逻辑+对象转换）
     * <p>
     * 逻辑：先查Redis → 命中直接返回 → 未命中查数据库 → 存入Redis → 返回结果
     */
    @Override
    public Result queryTypeList() {
        // 1. 从Redis查询缓存
        List<String> typeJsonList = stringRedisTemplate.opsForList().range(CACHE_SHOP_TYPE_KEY, 0, -1);
        // 2. 缓存命中：将JSON字符串列表转换为ShopType列表并返回
        if (typeJsonList != null && !typeJsonList.isEmpty()) {
            List<ShopType> shopTypeList = typeJsonList.stream()
                    // 利用Hutool的JSONUtil将JSON字符串转为ShopType对象
                    .map(json -> JSONUtil.toBean(json, ShopType.class))
                    .collect(Collectors.toList());
            return Result.ok(shopTypeList);
        }
        // 3. 缓存未命中：从数据库查询（按sort字段排序，保证顺序一致）
        List<ShopType> shopTypeList = query().orderByAsc("sort").list();
        // 4. 数据库无数据（异常情况处理）
        if (shopTypeList == null || shopTypeList.isEmpty()) {
            return Result.fail("店铺类型数据不存在");
        }
        // 5. 将数据库查询结果存入Redis（转为JSON字符串列表）
        List<String> jsonList = shopTypeList.stream()
                // 利用Hutool的JSONUtil将对象转为JSON字符串
                .map(JSONUtil::toJsonStr)
                .collect(Collectors.toList());
        stringRedisTemplate.opsForList().rightPushAll(CACHE_SHOP_TYPE_KEY, jsonList);
        stringRedisTemplate.expire(CACHE_SHOP_TYPE_KEY, CACHE_SHOP_TYPE_TTL, TimeUnit.DAYS);
        // 6. 返回结果
        return Result.ok(shopTypeList);
    }
}
