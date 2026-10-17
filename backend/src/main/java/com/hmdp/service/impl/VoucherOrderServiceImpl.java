package com.hmdp.service.impl;

import cn.hutool.json.JSONUtil;
import com.hmdp.dto.OrderDTO;
import com.hmdp.dto.Result;
import com.hmdp.entity.VoucherOrder;
import com.hmdp.mapper.VoucherOrderMapper;
import com.hmdp.service.ISeckillVoucherService;
import com.hmdp.service.IVoucherOrderService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.RedisIdWorker;
import com.hmdp.utils.UserHolder;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;

import static com.hmdp.utils.RedisConstants.ORDER_PREFIX;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class VoucherOrderServiceImpl extends ServiceImpl<VoucherOrderMapper, VoucherOrder> implements IVoucherOrderService {

    private static final String LOCK_PREFIX="lock:order:";



    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    RedissonClient redissonClient;

    @Autowired
    StringRedisTemplate stringRedisTemplate;

    @Autowired
    IVoucherOrderService orderService;

    @Autowired
    RedisIdWorker redisIdWorker;

    @Autowired
    ISeckillVoucherService seckillVoucherService;

    private static final DefaultRedisScript<Long> SECKILL_SCRIPT;
    static {
        SECKILL_SCRIPT =new DefaultRedisScript<>();
        SECKILL_SCRIPT.setLocation(new ClassPathResource("seckill.lua"));
        SECKILL_SCRIPT.setResultType(Long.class);
    }

    @Override
    public Result seckillVoucher(Long voucherId) {
        Long id = UserHolder.getUser().getId();

        Long result = stringRedisTemplate.execute(
                SECKILL_SCRIPT,
                Collections.emptyList(),
                id.toString(), voucherId.toString()
        );
        if(result!=0){
            return Result.fail(result==1?"库存不足":"不可重复下单");
        }
        // 把任务传给阻塞队列
        OrderDTO order = new OrderDTO();
        order.setUserId(id);
        order.setVoucherId(voucherId);
        Long orderId = redisIdWorker.nextId(ORDER_PREFIX);
        order.setOrderId(orderId);
        kafkaTemplate.send("voucher_order",orderId.toString(), JSONUtil.toJsonStr(order));
//        long orderId = redisIdWorker.nextId("order");
        return Result.ok(orderId);
    }



//    @Override
//    public Result seckillVoucher(Long voucherId) {
//        //1.查询优惠券
//        SeckillVoucher voucher = seckillVoucherService.getById(voucherId);
//        //2.查不到就报错
//        if(voucher==null){
//            return Result.fail("优惠券不存在！");
//        }
//        //3.查优惠券时间是否开始
//        if (voucher.getBeginTime().isAfter(LocalDateTime.now())) {
//            return Result.fail("优惠券秒杀尚未开始秒杀！");
//        }
//        //4.查优惠券时间是否结束
//        if (voucher.getEndTime().isBefore(LocalDateTime.now())) {
//            return Result.fail("优惠券秒杀已结束！");
//        }
//        //5.查库存
//        Integer stock = voucher.getStock();
//        if(stock<1){
//            return Result.fail("优惠券库存不足");
//        }
//        Long userId= UserHolder.getUser().getId();
//        RLock lock = redissonClient.getLock(LOCK_PREFIX + userId);
//        boolean isLock=lock.tryLock();
//        //获取锁失败就返回错误
//        if(!isLock){
//            return Result.fail("不可重复下单");
//        }
//        try {
//            return orderService.createVoucherOrder(voucherId);
//        } finally {
//            lock.unlock();
//        }
//
//    }

    @Transactional
    public Result createVoucherOrder(Long voucherId) {
        //查是否下单过
        Long userId= UserHolder.getUser().getId();
        Integer count = query().eq("user_id", userId).eq("voucher_id", voucherId).count();
        if(count>0){
            return Result.fail("单个用户只能购买一张优惠券");
        }

        //这里需要加悲观锁，是用户id  toString .intern 为锁的
        //6.扣库存
        boolean success = seckillVoucherService.update().setSql("stock = stock - 1").
                eq("voucher_id", voucherId)
                .gt("stock",0).update();
        if(!success){
            return Result.fail("库存不足");

        }
        //7.生成订单
        VoucherOrder voucherOrder = new VoucherOrder();

        long orderId = redisIdWorker.nextId("order");


        voucherOrder.setVoucherId(voucherId);
        voucherOrder.setId(orderId);
        voucherOrder.setUserId(userId);

        save(voucherOrder);

        return Result.ok(orderId);
    }
}
