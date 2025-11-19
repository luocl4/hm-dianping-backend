package com.hmdp.security;

import com.hmdp.dto.UserDTO;
import com.hmdp.utils.UserHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

//拦截器
@Component
public class LoginInterceptor implements HandlerInterceptor {

//    ①原来session的写法
//    @Override
//    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
////        System.out.println("拦截器中的preHandle");
////        1.从request中获取session
//        HttpSession session = request.getSession();
////        2.获取session中的用户
//        UserDTO userDTO = (UserDTO) session.getAttribute("user");
////        3.判断用户是否存在
//        if (userDTO == null) {
////            4.如果用户不存在，拦截,返回401
//            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
//            return false;
//        }
////        5.存在，则把用户信息放到ThreadLocal，用到com.hmdp.utils.UserHolder工具类
//        UserHolder.saveUser(userDTO);
//
    /// /        6.放行
//        return true;
//    }

//    private StringRedisTemplate stringRedisTemplate;
//
//    public LoginInterceptor(StringRedisTemplate stringRedisTemplate) {
//        this.stringRedisTemplate = stringRedisTemplate;  //@Component 注解的东西里面要自己写构造函数，才能用StringRedisTemplate
//    }
//
//    //    ②改为使用redis的写法
//    @Override
//    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

    /// /        1.从请求头中获取token
//        String token = request.getHeader("authorization");
//        if (token == null) {
//            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
//            return false;
//        }

    /// /        2.从redis中获得token对应的用户信息
//        Map<Object, Object> userMap = stringRedisTemplate.opsForHash().entries(LOGIN_USER_KEY + token);
//        if (userMap.isEmpty()) {
//            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
//            return false;
//        }
//        UserDTO userDTO = BeanUtil.fillBeanWithMap(userMap, new UserDTO(), false);  //由于我们存的是map，所以要把map转为userDTO，用到hutool中的fillBeanWithMap
//        UserHolder.saveUser(userDTO);
//        stringRedisTemplate.expire(LOGIN_USER_KEY + token, LOGIN_USER_TTL, TimeUnit.MINUTES);  //每当有用户请求，就刷新一次用户活跃这个key的有效期，模拟就是用户一段时间不登录才会失效
//        return true;
//    }

//    ③ 把一个拦截器改成两个之后的代码
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        UserDTO userDTO = UserHolder.getUser();
        if (userDTO == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return false;
        }
        return true;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
//        System.out.println("拦截器中的postHandle");
        HandlerInterceptor.super.postHandle(request, response, handler, modelAndView);
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
//        System.out.println("拦截器中的afterCompletion");
        UserHolder.removeUser();
    }
}
