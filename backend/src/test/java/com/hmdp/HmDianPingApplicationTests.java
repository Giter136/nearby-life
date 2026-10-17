package com.hmdp;

import com.hmdp.entity.Shop;
import com.hmdp.service.impl.ShopServiceImpl;
import com.hmdp.utils.RedisIdWorker;
import io.lettuce.core.api.sync.RedisGeoCommands;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@SpringBootTest
class HmDianPingApplicationTests {

    @Autowired
    StringRedisTemplate stringRedisTemplate;

    @Autowired
    RedisIdWorker redisIdWorker;

    @Autowired
    ShopServiceImpl shopService;

    @Test
    public void test1() throws InterruptedException {
        shopService.setData2Redis(1L,30L);
    }
    private static final ExecutorService es = Executors.newFixedThreadPool(300);

    @Test
    public void test2() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(300);
        Runnable r = ()->{
            for (int i = 0; i < 100; i++) {
                long id=redisIdWorker.nextId("order");
                System.out.println("id:"+id);
            }
            latch.countDown();
        };
        long begin = System.currentTimeMillis();
        for (int i = 0; i < 300; i++) {
            es.submit(r);
        }
        latch.await();
        long finish =System.currentTimeMillis();
        System.out.println(finish-begin);
    }

    @Test
    public void test3(){
        List<Shop> shopList = shopService.list();
        //将店铺按照类型分开
        Map<Long, List<Shop>> collect = shopList.stream().collect(Collectors.groupingBy(Shop::getTypeId));

        for (Map.Entry<Long, List<Shop>> entry : collect.entrySet()) {
            Long typeId = entry.getKey();
            List<Shop> shops = entry.getValue();
            String key = "shop:geo:"+typeId;

            for (Shop shop : shops) {
                stringRedisTemplate.opsForGeo().add(key,new Point(shop.getX(),shop.getY()),shop.getId().toString());
            }

        }

    }
}
