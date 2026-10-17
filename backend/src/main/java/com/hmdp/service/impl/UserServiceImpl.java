package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.LoginFormDTO;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.User;
import com.hmdp.mapper.UserMapper;
import com.hmdp.service.IUserService;
import com.hmdp.utils.RegexUtils;
import com.hmdp.utils.UserHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.BitFieldSubCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpSession;
import java.nio.file.attribute.UserPrincipalLookupService;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
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
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Result send(String phone, HttpSession session) {
        //1.校验电话
        if (RegexUtils.isPhoneInvalid(phone)) {
            return Result.fail("手机号不合法");
        }
        //2.生成随机验证码
        String code=RandomUtil.randomNumbers(6);

        //3.保存验证码到 redis 中,code:手机号为 key
        String key = LOGIN_CODE_KEY +phone;
        stringRedisTemplate.opsForValue().set(key,code,LOGIN_CODE_TTL, TimeUnit.MINUTES);

        //4.发送验证码fake
        log.debug("发送验证码成功，{}",code);
        return Result.ok();
    }

    @Override
    public Result login(LoginFormDTO loginForm, HttpSession session) {
        //1,鉴别手机号
        String phone = loginForm.getPhone();
        if(RegexUtils.isPhoneInvalid(phone)){
            return Result.fail("手机号不合法");
        }
        //2.检验验证码
        String code = loginForm.getCode();
        if (code==null || !code.equals(stringRedisTemplate.opsForValue().get(LOGIN_CODE_KEY+phone))) {
            return Result.fail("验证码错误");
        }
        //3.从数据库找人
        User user = query().eq("phone",phone).one();

        //4.看用户是否存在
        if (user==null) {
            user= createUserWithPhone(phone);
            //5.保存用户到数据库中
            save(user);
        }

        //6.用户保存到 redis 中
        //session.setAttribute("user", BeanUtil.copyProperties(user,UserDTO.class));
        //7.生成随机 token
        String token = UUID.randomUUID().toString();
        //8.将 User 转为 UserDTO
        UserDTO userDTO = BeanUtil.copyProperties(user,UserDTO.class);
        //9.映射对象为 map 并存到 redis 中
        Map<String,Object> userMap = BeanUtil.beanToMap(userDTO,new HashMap<>(),
                CopyOptions.create().ignoreNullValue().setFieldValueEditor((fieldName,fieldValue) -> fieldValue.toString() )
                );//TODO 待学
        String userKey= LOGIN_USER_KEY +token;
        stringRedisTemplate.opsForHash().putAll(userKey,userMap);

        //10.设置 token 有效期
        stringRedisTemplate.expire(userKey,LOGIN_USER_TTL ,TimeUnit.SECONDS);
        return Result.ok(token);
    }

    @Override
    public Result sign() {
        //1.获取当前用户
        UserDTO user = UserHolder.getUser();
        if (user==null){
            return Result.fail("请先登录！");
        }
        Long userId = user.getId();
        //2.获取今天年月
        LocalDateTime now = LocalDateTime.now();
        String yearAndMonth = now.format(DateTimeFormatter.ofPattern(":yyyyMM"));
        //3.获取今天日期
        int dayOfMonth = now.getDayOfMonth();
        //4.拼接 key
        String key = "sign:"+userId+yearAndMonth;
        //5.签到
        stringRedisTemplate.opsForValue().setBit(key,dayOfMonth-1,true);
        return Result.ok();
    }

    @Override
    public Result signCount() {
        //1.获取当前用户
        UserDTO user = UserHolder.getUser();
        if (user==null){
            return Result.fail("请先登录！");
        }
        Long userId = user.getId();
        //2.获取今天年月
        LocalDateTime now = LocalDateTime.now();
        String yearAndMonth = now.format(DateTimeFormatter.ofPattern(":yyyyMM"));
        //3.获取今天日期
        int dayOfMonth = now.getDayOfMonth();
        //4.拼接 key
        String key = "sign:"+userId+yearAndMonth;
        //5.获取签到记录
        List<Long> signRecord = stringRedisTemplate.opsForValue().bitField(key, BitFieldSubCommands.create()
                .get(BitFieldSubCommands.BitFieldType.unsigned(dayOfMonth)).valueAt(0)
        );
        //6.循环逻辑获取连续签到天数
        if (signRecord==null||signRecord.isEmpty()){
            return Result.ok(0);
        }
        int constantSignDay=0;
        Long l = signRecord.get(0);
        while(true){
            if((l&1)==0){
                break;
            }
            else {
                constantSignDay++;
            }
            l=l>>>1;
        }
        return Result.ok(constantSignDay);
    }

    private User createUserWithPhone(String phone) {
        User user = new User();
        user.setPhone(phone);
        String nickName="用户"+phone.substring(0,3)+"****"+phone.substring(phone.length()-4);
        user.setNickName(nickName);
        return user;
    }

}
