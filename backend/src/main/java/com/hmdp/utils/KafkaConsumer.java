package com.hmdp.utils;

import cn.hutool.json.JSONUtil;
import com.hmdp.dto.OrderDTO;
import com.hmdp.entity.VoucherOrder;
import com.hmdp.service.ISeckillVoucherService;
import com.hmdp.service.IVoucherOrderService;
import com.hmdp.service.impl.VoucherOrderServiceImpl;
import org.apache.kafka.common.internals.Topic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class KafkaConsumer {

    private ISeckillVoucherService seckillVoucherService;

    private IVoucherOrderService voucherOrderService;

    @Autowired
    public KafkaConsumer(ISeckillVoucherService seckillVoucherService, StringRedisTemplate stringRedisTemplate, IVoucherOrderService voucherOrderService) {
        this.seckillVoucherService = seckillVoucherService;
        this.voucherOrderService = voucherOrderService;
    }

    @Transactional
    @KafkaListener(topics = "voucher_order")
    public void processVoucherOrder(String orderJson){
        //处理订单
        //获得订单信息
        OrderDTO order = JSONUtil.toBean(orderJson, OrderDTO.class);
        Long orderId = order.getOrderId();
        Long voucherId = order.getVoucherId();
        Long userId = order.getUserId();
        VoucherOrder voucherOrder = new VoucherOrder();
        voucherOrder.setId(orderId);
        voucherOrder.setVoucherId(voucherId);
        voucherOrder.setUserId(userId);
        voucherOrder.setStatus(1);

        boolean update = seckillVoucherService.update().setSql("stock = stock -1").eq("voucher_id", voucherId).gt("stock", 0).update();
        if( !update ){
            throw new IllegalStateException("库存不足或优惠券不存在");
        }
        if( !voucherOrderService.save(voucherOrder) ){
            throw new IllegalStateException("保存订单失败");
        }

    }
}
