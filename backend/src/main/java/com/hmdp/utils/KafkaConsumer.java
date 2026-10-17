package com.hmdp.utils;

import cn.hutool.json.JSONUtil;
import com.hmdp.dto.OrderDTO;
import com.hmdp.entity.VoucherOrder;
import com.hmdp.service.ISeckillVoucherService;
import com.hmdp.service.IVoucherOrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
public class KafkaConsumer {

    private final ISeckillVoucherService seckillVoucherService;

    private final IVoucherOrderService voucherOrderService;

    public KafkaConsumer(ISeckillVoucherService seckillVoucherService,
                         IVoucherOrderService voucherOrderService) {
        this.seckillVoucherService = seckillVoucherService;
        this.voucherOrderService = voucherOrderService;
    }

    @Transactional
    @KafkaListener(topics = "voucher_order")
    public void processVoucherOrder(String orderJson) {
        OrderDTO order = JSONUtil.toBean(orderJson, OrderDTO.class);

        VoucherOrder voucherOrder = new VoucherOrder();
        Long orderId = order.getOrderId();
        voucherOrder.setId(orderId);
        Long voucherId = order.getVoucherId();
        voucherOrder.setVoucherId(voucherId);
        Long userId = order.getUserId();
        voucherOrder.setUserId(userId);
        voucherOrder.setStatus(1);

        // 先插订单：主键或唯一索引冲突说明这条消息已经处理过，直接返回（幂等）
        try {
            voucherOrderService.save(voucherOrder);
        } catch (DuplicateKeyException e) {
            log.warn("重复消费，已忽略: orderId={}", orderId);
            return;
        }

        // 插入成功后才扣库存；扣减失败则抛异常，整个事务回滚（订单插入也会撤销）
        boolean updated = seckillVoucherService.update()
                .setSql("stock = stock - 1")
                .eq("voucher_id", voucherId)
                .gt("stock", 0)
                .update();
        if (!updated) {
            throw new IllegalStateException("库存不足或优惠券不存在: voucherId=" + order.getVoucherId());
        }
    }
}
