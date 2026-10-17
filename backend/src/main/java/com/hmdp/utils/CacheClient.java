package com.hmdp.utils;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.hmdp.entity.Shop;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static com.hmdp.utils.RedisConstants.*;

@Slf4j
@Component
public class CacheClient {


    private final StringRedisTemplate stringRedisTemplate;

    public CacheClient(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    //1。 set ，将任意 java 对象序列化并存储为 String 类型的 kv 对，并且可以设置 TTL
    public void set(String key, Object object, Long time, TimeUnit unit){
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(object),time,unit);
    }
    //2. setWithExpireTime ，使用逻辑过期方式
    public void setWithExpireTime(String key, Object object, Long time, TimeUnit unit){
        RedisData<Object> redisData=new RedisData<>();
        redisData.setData(object);
        redisData.setExpireTime(LocalDateTime.now().plusSeconds(unit.toSeconds(time)));
        stringRedisTemplate.opsForValue().set(key,JSONUtil.toJsonStr(redisData));
    }
    //3. 根据指定 key 查询缓存，并反序列化为指定类型。利用缓存空值的方式解决缓存穿透问题
    public <T,ID> T queryWithPassThrough(String keyPrev, ID id, Class<T> type, Function<ID,T> function, Long time, TimeUnit unit){
        String key = keyPrev+ id;
        String json = stringRedisTemplate.opsForValue().get(key);
        if(StrUtil.isNotBlank(json)){
            return JSONUtil.toBean(json,type);
        }
        if(json!=null){
            return null;
        }
        //3.1 查数据库
        T data  = function.apply(id);
        if(data==null){
            this.set(key,"",time,unit);
            return null;
        }
        this.set(key,data,time,unit);
        return data;
    }
    //4. 用互斥锁和逻辑过期来解决缓存击穿
    private static final ExecutorService CACHE_REBUILD_EXECUTOR= Executors.newFixedThreadPool(5);

    public <T,ID> T queryWithLogicExpire(String keyPrev, String lockPrev,ID id, Class<T> type, Function<ID,T> function, Long time, TimeUnit unit) {
        //目标是用逻辑过期来重建缓存
        //1.从 redis 中获取
        String key = keyPrev+id;
        String json = stringRedisTemplate.opsForValue().get(key);
        //2. 若从 redis 中搜不到就返回空
        if(StrUtil.isBlank(json)){
            return null;
        }
        //3. 找到就转化为 RedisData 对象
        RedisData<T> redisData = JSONUtil.toBean(json,RedisData.class);
        T t= JSONUtil.toBean((JSONObject) redisData.getData(),type);
        //4.若未过期就直接返回
        if(redisData.getExpireTime().isAfter(LocalDateTime.now())){
            return t;
        }
        //5. 过期，开始抢锁
        boolean isLock=tryLock(lockPrev+id);
        //6. 如果没拿到锁就返回旧数据
        if(!isLock){
            return t;
        }
        //7.拿到就开始开启一个线程开始重建缓存，但是要做 double check 防止别的线程已经完成重建
        redisData= JSONUtil.toBean(stringRedisTemplate.opsForValue().get(key),RedisData.class);
        if(redisData.getExpireTime().isAfter(LocalDateTime.now())){
            t= JSONUtil.toBean((JSONObject) redisData.getData(),type);
            return t;
        }
        //8.调用线程池开始做缓存重建
        CACHE_REBUILD_EXECUTOR.submit(()->{
            try {
                T data = function.apply(id);
                this.setWithExpireTime(key,data,time,unit);
            } catch (Exception e) {
                log.error("缓存重建失败，key={}", key, e);
            } finally {
                unLock(lockPrev+id);
            }
        });
        return t;
    }

    //抢锁函数
    private boolean tryLock(String key){
        Boolean b = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", 10, TimeUnit.SECONDS);
        return BooleanUtil.isTrue(b);
    }
    //解锁函数
    private void unLock(String key){
        stringRedisTemplate.delete(key);
    }
}
