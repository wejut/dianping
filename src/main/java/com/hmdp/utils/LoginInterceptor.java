package com.hmdp.utils;

import cn.hutool.json.JSONUtil;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Component
@Slf4j
public class LoginInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        UserDTO user = UserHolder.getUser();
        if (user == null) {
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write(JSONUtil.toJsonStr(Result.fail("未登录")));
            return false;
        }
        return true;
    }
}
