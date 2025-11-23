package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.LoginFormDTO;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.User;
import com.hmdp.mapper.UserMapper;
import com.hmdp.service.IUserService;
import com.hmdp.utils.RegexUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.hmdp.constant.RedisConstants.*;
import static com.hmdp.constant.SystemConstants.USER_NICK_NAME_PREFIX;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 发送手机验证码
     *
     * @return com.hmdp.dto.Result
     */
    @Override
    public Result sendCode(String phone, HttpSession session) {
//        1.接收前端提交的手机号并且校验，用到写好的工具类 com.hmdp.utils.RegexUtils
        if (RegexUtils.isPhoneInvalid(phone)) {
//            2.不符合则返回错误信息
            return Result.fail("无效手机号");
        }
//        3.符合则生成六位验证码，用到hutool的生成随机数字的api
        String code = RandomUtil.randomNumbers(6);
////        4.保存验证码到session
//        session.setAttribute("code", code);
//        4.改为保存验证码到redis，2min有效期，前缀为login:code:
        stringRedisTemplate.opsForValue().set(LOGIN_CODE_KEY + phone, code, LOGIN_CODE_TTL, TimeUnit.MINUTES);
//        5.发送验证码，返回ok，这里假装做了
        log.debug("发送短信验证码成功，验证码{}", code);
        return Result.ok();
    }

    /**
     * 登录功能
     *
     * @param loginForm 登录参数，包含手机号、验证码；或者手机号、密码
     */
    @Override
    public Result login(LoginFormDTO loginForm, HttpSession session) {
//        1.校验手机号
        String phone = loginForm.getPhone();
        if (RegexUtils.isPhoneInvalid(phone)) {
            return Result.fail("无效手机号");
        }
////        2.校验验证码
        String formCode = loginForm.getCode();  //获得表单中的发送过来的验证码
//        String sessionCode = (String) session.getAttribute("code");  //获得存在session中的正确的验证码
//        2.改为从redis中获得sessionCode
        String sessionCode = stringRedisTemplate.opsForValue().get(LOGIN_CODE_KEY + phone);
        if (sessionCode == null || formCode == null || !sessionCode.equals(formCode)) {
//            3.不一致，则返回错误信息
            return Result.fail("验证码错误");
        }
//        4.一致，则根据手机号查询用户，语句是：select * from tb_user where phone=？;
//        用mybatis-plus的https://baomidou.com/guides/data-interface/#chain的query实现
        User user = query().eq("phone", phone).one();
//        5.判断用户是否存在
        if (user == null) {
//            6.不存在，则创建新用户，保存用户到数据库
//            用的是mybatis-plus的https://baomidou.com/guides/data-interface/#save实现
//            在save之前需要构建一个用于存入的对象，用的是Lombok的@Builder注解
//            nickName设定为"user_"开头的，再加10位乱码，乱码用hutool生成
            user = User.builder().phone(phone).nickName(USER_NICK_NAME_PREFIX + RandomUtil.randomString(10)).build();
            save(user);
        }
////        7.确保存在后，则保存用户到session，不完整保存，要用BeanUtils.copyProperties转化user类型到userDTO类型，仅保存核心非隐私信息
//        UserDTO userDTO = new UserDTO();
//        BeanUtils.copyProperties(user, userDTO);
//        session.setAttribute("user", userDTO);
//        7.改为保存用户到redis，然后生成并返回token到客户端
        String token = UUID.randomUUID().toString();  //用hutool工具包的uuid的方式生成token
        UserDTO userDTO = BeanUtil.copyProperties(user, UserDTO.class);
        Map<String, String> userMap = new HashMap<>();
        userMap.put("id", String.valueOf(userDTO.getId()));
        userMap.put("nickName", userDTO.getNickName());
        userMap.put("icon", userDTO.getIcon());
        //由于hash结构的putAll函数要存的是map类型，所以要转为map
        stringRedisTemplate.opsForHash().putAll(LOGIN_USER_KEY + token, userMap);
        stringRedisTemplate.expire(LOGIN_USER_KEY + token, LOGIN_USER_TTL, TimeUnit.MINUTES);  //设置有效期
        return Result.ok(token);
    }
}
