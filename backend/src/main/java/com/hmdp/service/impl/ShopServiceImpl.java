package com.hmdp.service.impl;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;
import com.hmdp.mapper.ShopMapper;
import com.hmdp.service.IShopService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.CacheClient;
import com.hmdp.utils.RedisConstants;
import com.hmdp.utils.RedisData;
import com.hmdp.utils.SystemConstants;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoResult;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.domain.geo.GeoReference;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.RedisConstants.*;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements IShopService {

    @Autowired
    StringRedisTemplate stringRedisTemplate;

    @Autowired
    CacheClient cacheClient;

    @Override
    public Result queryById(Long id) {
        Shop shop = cacheClient.queryWithPassThrough(CACHE_SHOP_KEY,id,Shop.class,this::getById, CACHE_SHOP_TTL,TimeUnit.MINUTES);
        if (shop==null){
            return Result.fail("请求错误");
        }
        else{
            return Result.ok(shop);
        }
    }

    private static final ExecutorService CACHE_REBUILD_EXECUTOR= Executors.newFixedThreadPool(5);

    public Shop queryWithLogicExpire(Long id) {
        //目标是用逻辑过期来重建缓存
        //1.从 redis 中获取
        String shopKey = CACHE_SHOP_KEY+id;
        String jsonShop= stringRedisTemplate.opsForValue().get(shopKey);
        //2. 若从 redis 中搜不到就返回空
        if(StrUtil.isBlank(jsonShop)){
            return null;
        }
        //3. 找到就转化为 RedisData 对象
        RedisData redisData = JSONUtil.toBean(jsonShop,RedisData.class);
        Shop shop= JSONUtil.toBean((JSONObject) redisData.getData(),Shop.class);
        //4.若未过期就直接返回
        if(redisData.getExpireTime().isAfter(LocalDateTime.now())){
            return shop;
        }
        //5. 过期，开始抢锁
        boolean isLock=tryLock(LOCK_SHOP_KEY+id);
        //6. 如果没拿到锁就返回旧数据
        if(!isLock){
            return shop;
        }
        //7.拿到就开始开启一个线程开始重建缓存，但是要做 double check 防止别的线程已经完成重建
        if(redisData.getExpireTime().isAfter(LocalDateTime.now())){
            return shop;
        }
        //8.调用线程池开始做缓存重建
        CACHE_REBUILD_EXECUTOR.submit(()->{
            try {
                setData2Redis(id,30L);
            } catch (Exception e) {
                throw new RuntimeException(e);
            } finally {
                unLock(LOCK_SHOP_KEY+id);
            }
        });
        return shop;


    }


    public Shop queryWithMutex(Long id) {
        // 目的是解决缓存击穿（热点数据缓存失效导致大量请求短时间打到数据库）
        // 解决方法：1.用  redis 分布式锁来使得缓存重建只有一个线程在做防止大量请求攻击
        //         2.没抢到 redis 分布式锁的就等待之后重试
        //         3.缓存重建完毕之后解锁

        //已经解决缓存穿透（一直查询数据库中不存在的数据）
        //1.从 redis 中查询
        String key= CACHE_SHOP_KEY+id;
        String jsonShop=stringRedisTemplate.opsForValue().get(key);
        //2.查询到直接转化为对象后返回
        Shop shop;
        if(StrUtil.isNotBlank(jsonShop)){
            shop = JSONUtil.toBean(jsonShop, Shop.class);
            return shop;
        }
        if(jsonShop!=null){
            return null;
        }
        //3.没有就从数据库中查找
        String lockKey=LOCK_SHOP_KEY+id;
        try {
            boolean isLock=tryLock(lockKey);
            int retry=0;
            if (!isLock) {
                while(retry++<50){
                    String newJsonShop =  stringRedisTemplate.opsForValue().get(key);
                    if(StrUtil.isNotBlank(newJsonShop)){
                        shop = JSONUtil.toBean(newJsonShop, Shop.class);
                        return shop;
                    }
                    else {
                        try {
                            Thread.sleep(50);
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
                return null;
            }
            shop=getById(id);
            Thread.sleep(200);
            //4.找不到就返回错误
            if(shop==null){
                stringRedisTemplate.opsForValue().set(key,"",CACHE_NULL_TTL, TimeUnit.MINUTES);
                unLock(lockKey);
                return null;
            }
            else{
                stringRedisTemplate.opsForValue().set(key,JSONUtil.toJsonStr(shop),CACHE_SHOP_TTL, TimeUnit.MINUTES);
            }
        } catch (RuntimeException | InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            unLock(lockKey);
        }
        //5.找到就返回店铺
        return shop;
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

    public void setData2Redis(Long id,Long time) throws InterruptedException {
        //1. 获取数据
        Thread.sleep(200);
        Shop shop= getById(id);
        //2. 创建 RedisData 类
        RedisData<Shop> redisData= new RedisData<>();
        redisData.setExpireTime(LocalDateTime.now().plusSeconds(time));
        redisData.setData(shop);
        //3..存入数据到 redis
        stringRedisTemplate.opsForValue().set(CACHE_SHOP_KEY+id,JSONUtil.toJsonStr(redisData));
    }

    @Override
    @Transactional
    public Result update(Shop shop) {
        Long id = shop.getId();
        if(id==null){
            return Result.fail("商铺 id 不能为空");
        }
        updateById(shop);
        stringRedisTemplate.delete(CACHE_SHOP_KEY+id);
        return Result.ok();
    }

    //缓存，查询
    @Override
    public Result queryShopByType(Integer typeId, Integer current, Double x, Double y) {
        if(x==null||y==null){
            Page<Shop> page = query()
                    .eq("type_id", typeId)
                    .page(new Page<>(current, SystemConstants.DEFAULT_PAGE_SIZE));
            // 返回数据
            return Result.ok(page.getRecords());
        }
        //计算分页参数
        int from = (current - 1) * DEFAULT_BATCH_SIZE;
        int end = current * DEFAULT_BATCH_SIZE;

        String key = "shop:geo:"+typeId;
        //按距离查询
        GeoResults<RedisGeoCommands.GeoLocation<String>> results = stringRedisTemplate.opsForGeo().search(
                key, GeoReference.fromCoordinate(x, y),
                new Distance(5000),
                RedisGeoCommands.GeoSearchCommandArgs.newGeoSearchArgs().includeDistance().limit(end)
        );
        if (results==null) {
            return Result.ok(Collections.emptyList());
        }
        List<GeoResult<RedisGeoCommands.GeoLocation<String>>> content = results.getContent();
        if (content.size()<=from) {
            return Result.ok(Collections.emptyList());
        }
        ArrayList<Long> ids = new ArrayList<>(content.size());
        Map<String,Distance> distanceMap = new HashMap<>(content.size());
        content.stream().skip(from).forEach(result->{
            String shopIdStr = result.getContent().getName();
            ids.add(Long.valueOf(shopIdStr));

            Distance distance = result.getDistance();
            distanceMap.put(shopIdStr,distance);
        });
        String idStr = StrUtil.join(",", ids);
        List<Shop> shops = query().in("id", ids).last("ORDER BY FIELD(id," + idStr + ")").list();
        for (Shop shop : shops) {
            shop.setDistance(distanceMap.get(shop.getId().toString()).getValue());
        }
        return Result.ok(shops);
    }

    @Override
    public Result queryShopByName(Integer typeId, Integer current, Double x, Double y) {
        return null;
    }
}
