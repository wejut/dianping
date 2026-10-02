package com.hmdp.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.Result;
import com.hmdp.entity.SeckillVoucher;
import com.hmdp.entity.VoucherOrder;
import com.hmdp.mapper.VoucherOrderMapper;
import com.hmdp.service.ISeckillVoucherService;
import com.hmdp.service.IVoucherOrderService;
import com.hmdp.utils.RedisIdWorker;
import com.hmdp.utils.UserHolder;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.Collections;

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

    @Resource
    private ISeckillVoucherService seckillVoucherService;
    @Resource
    private RedisIdWorker redisIdWorker;
    /**
     * 注入自己，保证 createVoucherOrder 走 Spring 代理，@Transactional 生效
     */
    @Resource
    @Lazy
    private IVoucherOrderService voucherOrderService;

    @Override
    public Result seckillVoucher(Long voucherId) {
        // 1.查询秒杀券（库存、开始/结束时间在 tb_seckill_voucher）
        SeckillVoucher voucher = seckillVoucherService.getById(voucherId);
        if (voucher == null) {
            return Result.fail("秒杀券不存在");
        }
        // 2.判断秒杀是否开始
        if (voucher.getBeginTime().isAfter(LocalDateTime.now())) {
            return Result.fail("秒杀尚未开始");
        }
        // 3.判断秒杀是否结束
        if (voucher.getEndTime().isBefore(LocalDateTime.now())) {
            return Result.fail("秒杀已经结束");
        }
        // 4.判断库存是否足够
        if (voucher.getStock() < 1) {
            return Result.fail("库存不足");
        }
        if (UserHolder.getUser() == null) {
            return Result.fail("未登录");
        }
        Long userId = UserHolder.getUser().getId();
        // 悲观锁：同一用户串行，intern 保证相同 id 用同一把锁
        synchronized (userId.toString().intern()) {
            return voucherOrderService.createVoucherOrder(voucherId);
        }
    }

    @Override
    @Transactional
    public Result createVoucherOrder(Long voucherId) {
        Long userId = UserHolder.getUser().getId();
        // 一人一单：查 tb_voucher_order 是否已有该用户该券的订单
        int count = query().eq("user_id", userId).eq("voucher_id", voucherId).count();
        if (count > 0) {
            return Result.fail("用户已经购买过一次");
        }
        // 扣减库存（stock > 0 防止超卖）
        boolean success = seckillVoucherService.update()
                .setSql("stock = stock - 1")
                .eq("voucher_id", voucherId)
                .gt("stock", 0)
                .update();
        if (!success) {
            return Result.fail("库存不足");
        }
        VoucherOrder voucherOrder = new VoucherOrder();
        long orderId = redisIdWorker.nextId("order");
        voucherOrder.setId(orderId);
        voucherOrder.setUserId(userId);
        voucherOrder.setVoucherId(voucherId);
        save(voucherOrder);
        return Result.ok(Collections.singletonMap("id", orderId));
    }
}
