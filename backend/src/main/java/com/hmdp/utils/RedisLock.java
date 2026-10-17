package com.hmdp.utils;

import cn.hutool.core.lang.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

public class RedisLock implements ILock{


    private static final String ID_PREFIX= UUID.randomUUID().toString(true)+"-";
    private String name;
    private final StringRedisTemplate stringRedisTemplate;
    private static final String KEY_PREFIX="lock:";
    private static final DefaultRedisScript<Long> UNLOCK_SCRIPT;
    static {
        UNLOCK_SCRIPT=new DefaultRedisScript<>();
        UNLOCK_SCRIPT.setLocation(new ClassPathResource("unlock.lua"));
        UNLOCK_SCRIPT.setResultType(Long.class);
    }

    public RedisLock(StringRedisTemplate stringRedisTemplate, String name) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.name = name;
    }

    @Override
    public boolean tryLock(long timeoutSec) {
        //1.用 setnx 获取锁
        String threadName = ID_PREFIX+Thread.currentThread().getName();
        Boolean flag = stringRedisTemplate.opsForValue().setIfAbsent(KEY_PREFIX + name, threadName,timeoutSec, TimeUnit.MILLISECONDS);
        return Boolean.TRUE.equals(flag);
    }

    @Override
    public void unlock() {
//        String threadName = ID_PREFIX+Thread.currentThread().getName();
//        String redisName=stringRedisTemplate.opsForValue().get(KEY_PREFIX + name);
//        if(threadName.equals(redisName)){
//            stringRedisTemplate.delete(KEY_PREFIX + name);
//        }

        stringRedisTemplate.execute(UNLOCK_SCRIPT,
                Collections.singletonList(KEY_PREFIX + name),
                ID_PREFIX+Thread.currentThread().getName()
                );
    }
}
