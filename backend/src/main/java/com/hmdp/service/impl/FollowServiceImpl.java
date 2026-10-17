package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.Follow;
import com.hmdp.entity.User;
import com.hmdp.mapper.FollowMapper;
import com.hmdp.service.IFollowService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.service.IUserService;
import com.hmdp.utils.UserHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class FollowServiceImpl extends ServiceImpl<FollowMapper, Follow> implements IFollowService {

    @Autowired
    IUserService userService;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Result follow(Long followUserId, boolean isFollow) {
        UserDTO user = UserHolder.getUser();
        if (user==null) {
            return Result.fail("请先登录");
        }
        Long userId = user.getId();
        if(isFollow){
            Follow follow = new Follow();
            follow.setUserId(userId);
            follow.setFollowUserId(followUserId);
            save(follow);
            stringRedisTemplate.opsForSet().add("follow:"+userId,followUserId.toString());
        }
        else {
            remove(new QueryWrapper<Follow>().eq("user_id",userId).eq("follow_user_id",followUserId));
            stringRedisTemplate.opsForSet().remove("follow:"+userId,followUserId.toString());
        }
        return Result.ok();
    }

    @Override
    public Result isFollow(Long followUserId) {
        UserDTO user = UserHolder.getUser();
        if (user==null) {
            return Result.fail("请先登录");
        }
        Long userId = user.getId();
        Integer count = query().eq("user_id", userId).eq("follow_user_id", followUserId).count();
        return Result.ok(count>0);
    }

    @Override
    public Result commonFollow(Long anotherUserId){
        UserDTO user = UserHolder.getUser();
        if (user==null) {
            return Result.fail("请先登录");
        }
        Long userId = user.getId();
        String key1="follow:"+userId.toString();
        String key2="follow:"+anotherUserId.toString();
        Set<String> intersect = stringRedisTemplate.opsForSet().intersect(key1, key2);
        if(intersect==null ||intersect.isEmpty()){
            return Result.ok(Collections.emptyList());
        }
        List<Long> ids = intersect.stream().map(Long::valueOf).collect(Collectors.toList());
        List<UserDTO> userDTOs = ids.stream().map(id -> BeanUtil.copyProperties(userService.getById(id),UserDTO.class)).collect(Collectors.toList());
        return Result.ok(userDTOs);
    }
}
