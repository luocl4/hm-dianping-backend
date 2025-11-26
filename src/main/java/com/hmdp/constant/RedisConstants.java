package com.hmdp.constant;

public class RedisConstants {
    //    存在redis里面的验证码的相关常量
    public static final String LOGIN_CODE_KEY = "login:code:";
    public static final Long LOGIN_CODE_TTL = 2L;

    //    存在redis里面的用户登录信息的的相关常量
    public static final String LOGIN_USER_KEY = "login:token:";
    public static final Long LOGIN_USER_TTL = 36000L;

    public static final Long CACHE_NULL_TTL = 2L;

    //    存放店铺详细信息的相关常量
    public static final Long CACHE_SHOP_TTL = 30L;
    public static final String CACHE_SHOP_KEY = "cache:shop:";

    //    存放店铺类型的常量
    public static final String CACHE_SHOP_TYPE_KEY = "cache:shopType:";
    public static final Long CACHE_SHOP_TYPE_TTL = 30L;

    //    互斥锁解决缓存击穿那里用到的setnx的相关常量
    public static final String LOCK_SHOP_KEY = "lock:shop:";
    public static final Long LOCK_SHOP_TTL = 10L;

    //    异步秒杀优化的相关常量
    public static final String SECKILL_STOCK_KEY = "seckill:stock:";

    //    博客点赞相关常量
    public static final String BLOG_LIKED_KEY = "blog:liked:";
    
    public static final String FEED_KEY = "feed:";
    public static final String SHOP_GEO_KEY = "shop:geo:";
    public static final String USER_SIGN_KEY = "sign:";
}
