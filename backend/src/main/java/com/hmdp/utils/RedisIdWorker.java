package com.hmdp.utils;


import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
public class RedisIdWorker {

    private final static long INITIAL_TIMESTAMP=1786320000L;//初始时间戳2026.8.10，00:00
    private final static long BIT_COUNTS=32;//序列号比特位数
    private final StringRedisTemplate stringRedisTemplate;

    public RedisIdWorker(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    //创建全局唯一 id 生成器
    public long nextId(String keyPrefix){
        LocalDateTime now = LocalDateTime.now();
        //1.得到当前时间戳
        long timestamp=now.toEpochSecond(ZoneOffset.UTC)-INITIAL_TIMESTAMP;
        //2.使用 redis 生成序列号
        String date = now.format(DateTimeFormatter.ofPattern("yyyy:MM:dd"));
        Long count = stringRedisTemplate.opsForValue().increment("icr:" +keyPrefix+":"+date);
        return timestamp<<BIT_COUNTS | count;

    }



}
