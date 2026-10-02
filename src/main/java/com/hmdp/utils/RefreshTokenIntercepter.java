package com.hmdp.utils;

import cn.hutool.core.bean.BeanUtil;
import com.hmdp.dto.UserDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class RefreshTokenIntercepter implements HandlerInterceptor {
    @Autowired
    private RedisTemplate redisTemplate;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        //获取session->获取请求头中的token
        String token = request.getHeader("Authorization");
//        HttpSession session = request.getSession();
        //获取session中的数据->根据token获取redis用户信息
        Map<Object, Object> usermap = redisTemplate.opsForHash().entries(RedisConstants.LOGIN_USER_KEY + token);
        //判断是否存在，为空证明用户不存在，算作一种校验
        if (usermap.isEmpty()){
            return true;
        }
        //把map转成对象
        UserDTO userDTO = BeanUtil.fillBeanWithMap(usermap, new UserDTO(), false);
        //存储
        UserHolder.saveUser(userDTO);
        System.out.println("已登录用户 token=" + token);
        //刷新token有效期
        redisTemplate.expire(RedisConstants.LOGIN_USER_KEY+token,RedisConstants.LOGIN_USER_TTL, TimeUnit.MINUTES);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        UserHolder.removeUser();
    }
}
