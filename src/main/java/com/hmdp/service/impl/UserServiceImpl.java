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
import com.hmdp.utils.SystemConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import javax.servlet.http.HttpSession;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import com.hmdp.utils.RedisConstants;
import static com.hmdp.utils.RedisConstants.*;

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
    private RedisTemplate redisTemplate;

    @Override
    public Result sendCode(String phone, HttpSession session) {
        //check the number
        if(RegexUtils.isPhoneInvalid(phone)){
            //if no return error
            return Result.fail("手机号格式错误");
        }
        //generate code
        String code = RandomUtil.randomNumbers(6);
//        //save the code into session(redis)
//        session.setAttribute("code",code);
        //改为redis
        redisTemplate.opsForValue().set(LOGIN_CODE_KEY+phone, code,LOGIN_CODE_TTL, TimeUnit.MINUTES);
        log.info("createCode:"+code);
        return Result.ok();
    }

    @Override
    public Result login(LoginFormDTO loginFormdto, HttpSession session) {
        //验手机号
        if(RegexUtils.isPhoneInvalid(loginFormdto.getPhone())){
            //if no return error
            return Result.fail("手机号格式错误");
        }//验证session里验证码是否一致
//        Object cacheCode = session.getAttribute("code");
//        String code = loginFormdto.getCode();
        Object cacheCode = redisTemplate.opsForValue().get(LOGIN_CODE_KEY+loginFormdto.getPhone());
        String code = loginFormdto.getCode();
        if (code== null || !cacheCode.toString().equals(code)) {
            //不一致，报错
            return Result.fail("验证码错误");
        }
        //一致根据手机号查询用户
        User user = query().eq("phone",loginFormdto.getPhone()).one();
        if (user==null) {
            //没有，创建用户
           user = createTemporaryUser(loginFormdto.getPhone());
        }
        //有没有都得存用户数据进session
//        session.setAttribute("user", BeanUtil.copyProperties(user, UserDTO.class));
        //有没有都存进redis，用token当key不用phone防止内存泄露，生成token，把user转成map
        String token = UUID.randomUUID().toString(true);
        String tokenKey=LOGIN_USER_KEY+token;
        UserDTO userDTO = BeanUtil.copyProperties(user, UserDTO.class);
        Map<String, Object> usermap = BeanUtil.beanToMap(userDTO);
        redisTemplate.opsForHash().putAll(tokenKey,usermap);
        redisTemplate.expire(tokenKey, LOGIN_USER_TTL,TimeUnit.MINUTES);
        System.out.println("登录成功，token=" + token);
        log.info("登录成功，token={}", token);
        return Result.ok(token);
    }
        private User createTemporaryUser(String phone){

            User user1 = new User();
            user1.setPhone(phone);
            user1.setNickName(SystemConstants.USER_NICK_NAME_PREFIX +RandomUtil.randomString(10));
            user1.setCreateTime(LocalDateTime.now());
            save(user1);
            log.info("创建用户"+user1);
            return user1;
    }

}
