package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.hmdp.dto.Result;
import com.hmdp.entity.VoucherOrder;
import com.hmdp.mapper.VoucherOrderMapper;
import com.hmdp.service.ISeckillVoucherService;
import com.hmdp.service.IVoucherOrderService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.RedisIdWorker;
import com.hmdp.utils.UserHolder;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.aop.framework.AopContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.connection.stream.*;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Slf4j
@Service
public class VoucherOrderServiceImpl extends ServiceImpl<VoucherOrderMapper, VoucherOrder> implements IVoucherOrderService {

    @Resource
    private ISeckillVoucherService seckillVoucherService;

    @Resource
    private RedisIdWorker redisIdWorker;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private RedissonClient redissonClient;

    public static final DefaultRedisScript<Long> SECKILL_SCRIPT;
    static {
        SECKILL_SCRIPT = new DefaultRedisScript<>();
        SECKILL_SCRIPT.setLocation(new ClassPathResource("seckill.lua"));
        SECKILL_SCRIPT.setResultType(Long.class);
    }

    //阻塞队列
    private BlockingQueue<VoucherOrder> orderTasks = new ArrayBlockingQueue<>(1024 * 1024);
    //线程池
    public static final ExecutorService SECKILL_ORDER_EXECUTOR = Executors.newSingleThreadExecutor();

    //spring提供注解post construct 初始化的时候直接执行该方法
    @PostConstruct
    private void init(){
        SECKILL_ORDER_EXECUTOR.submit(new VoucherOrderHandler());
    }

    //启用线程任务
    private class VoucherOrderHandler implements Runnable{

        String queueName = "stream.orders";
        @Override
        public void run() {
            //由于有阻塞队列，可以一直执行，不会对cpu造成负担
            while (true){
                try {
                    //获取消息队列的订单信息
                    List<MapRecord<String, Object, Object>> list = stringRedisTemplate.opsForStream().read(
                            Consumer.from("g1", "c1"),
                            StreamReadOptions.empty().count(1).block(Duration.ofSeconds(2)),
                            StreamOffset.create(queueName, ReadOffset.lastConsumed())
                    );
                    //判断消息是否获取成功
                    if (list == null  || list.isEmpty()) {
                        continue;
                    }
                    //解析订单信息
                    MapRecord<String, Object, Object> record = list.get(0);
                    System.out.println("record = " + record);
                    Map<Object, Object> value = record.getValue();
                    VoucherOrder voucherOrder = BeanUtil.fillBeanWithMap(value, new VoucherOrder(), true);
                    //处理订单
                    handleVoucherOrder(voucherOrder);
                    //ACK确认
                    stringRedisTemplate.opsForStream().acknowledge(queueName, "g1", record.getId());
                    System.out.println("ACK!");
                } catch (Exception e) {
                    log.error("处理订单异常", e);
                    handlePendingList();
                }
            }
//        }

//        @Override
//        public void run() {
//            //由于有阻塞队列，可以一直执行，不会对cpu造成负担
//            while (true){
//                try {
//                    //等待阻塞队列里有数据
//                    VoucherOrder voucherOrder = orderTasks.take();
//                    //执行下单操作
//                    handleVoucherOrder(voucherOrder);
//                } catch (InterruptedException e) {
//                    log.error("处理订单异常", e);
//                }
//            }
//        }
    }
        private void handlePendingList() {
            while (true){
                try {
                    //获取PendingList消息队列的订单信息
                    List<MapRecord<String, Object, Object>> list = stringRedisTemplate.opsForStream().read(
                            Consumer.from("g1", "c1"),
                            StreamReadOptions.empty().count(1),
                            StreamOffset.create(queueName, ReadOffset.from("0"))
                    );
                    //判断消息是否获取成功
                    if (list == null  || list.isEmpty()) {
                        break;
                    }
                    //解析订单信息
                    MapRecord<String, Object, Object> record = list.get(0);
                    Map<Object, Object> value = record.getValue();
                    VoucherOrder voucherOrder = BeanUtil.fillBeanWithMap(value, new VoucherOrder(), true);
                    //处理订单
                    handleVoucherOrder(voucherOrder);
                    //ACK确认
                    stringRedisTemplate.opsForStream().acknowledge(queueName, "g1", record.getId());
                } catch (Exception e) {
                    log.error("处理PendingList异常", e);
                    try {
                        Thread.sleep(20);
                    } catch (InterruptedException ex) {
                        throw new RuntimeException(ex);
                    }
                }
            }

        }
        }
    //执行下单操作
    private void handleVoucherOrder(VoucherOrder voucherOrder){
        //不能从UserHolder里取值 因为是新线程
        Long userId = voucherOrder.getUserId();
        //Redisson锁
        RLock lock = redissonClient.getLock("lock:order:" + userId);
        boolean isLock = lock.tryLock();
        if (!isLock) {
            log.error("不许重复下单！");
        }
        try {
            proxy.createVoucherOrder(voucherOrder);
        } finally {
            lock.unlock();
        }

    }

    private IVoucherOrderService proxy;

    @Override
    public Result seckillVoucher(Long voucherId) {

        Long userId = UserHolder.getUser().getId();
        long orderId = redisIdWorker.nextId("order");
        //执行lua脚本
        Long result = stringRedisTemplate.execute(SECKILL_SCRIPT,
                Collections.emptyList(),
                voucherId.toString(), userId.toString(),
                String.valueOf(orderId)
        );

        //结果不为0 没有资格买
        int r = result.intValue();
        if (r != 0) {
            return  Result.fail(r == 1 ? "库存不足" : "不能重复下单");
        }

        //初始化代理变量（全局）
        proxy = (IVoucherOrderService)AopContext.currentProxy();

        // TODO 保存阻塞队列
        //创建阻塞队列：线程在队列里有值的时候才唤醒执行
        //orderTasks.add(voucherOrder);

        //返回订单id
        return Result.ok(orderId);
    }

    //    @Override
//    public Result seckillVoucher(Long voucherId) {
//
//        Long userId = UserHolder.getUser().getId();
//
//        //执行lua脚本
//        Long result = stringRedisTemplate.execute(SECKILL_SCRIPT,
//                Collections.emptyList(),
//                voucherId.toString(), userId.toString()
//        );
//
//        //结果不为0 没有资格买
//        int r = result.intValue();
//        if (r != 0) {
//            return  Result.fail(r == 1 ? "库存不足" : "不能重复下单");
//        }
//
//        //为0 , 生成订单信息， 把下单信息保存到阻塞队列
//        VoucherOrder voucherOrder = new VoucherOrder();
//        long orderId = redisIdWorker.nextId("order");
//        voucherOrder.setId(orderId);
//        voucherOrder.setUserId(userId);
//        voucherOrder.setVoucherId(voucherId);
//
//        //初始化代理变量（全局）
//        proxy = (IVoucherOrderService)AopContext.currentProxy();
//
//        // TODO 保存阻塞队列
//        //创建阻塞队列：线程在队列里有值的时候才唤醒执行
//        orderTasks.add(voucherOrder);
//
//        //返回订单id
//        return Result.ok(orderId);
//    }


    //    @Override
//    public Result seckillVoucher(Long voucherId) {
//        SeckillVoucher voucher = seckillVoucherService.getById(voucherId);
//
//        if (voucher.getBeginTime().isAfter(LocalDateTime.now())) {
//            return Result.fail("秒杀尚未开始！");
//        }
//
//        if (voucher.getEndTime().isBefore(LocalDateTime.now())) {
//            return Result.fail("秒杀已经结束！");
//        }
//
//        if (voucher.getStock() < 1) {
//            return Result.fail("库存不足！");
//        }
//
//
//        Long userId = UserHolder.getUser().getId();
//
//        //换分布式锁
////        synchronized (userId.toString().intern()){
////            IVoucherOrderService proxy = (IVoucherOrderService)AopContext.currentProxy();
////            return proxy.createVoucherOrder(voucherId);
////        }
//
//        //分布式锁
//        //SimpleRedisLock redisLock = new SimpleRedisLock("order:" + userId, stringRedisTemplate);
//        //boolean isLock = redisLock.tryLock(1200);
//
//        //Redisson锁
//        RLock lock = redissonClient.getLock("lock:order:" + userId);
//        boolean isLock = lock.tryLock();
//
//
//        if (!isLock) {
//            return Result.fail("不许重复下单！");
//        }
//
//        try {
//            IVoucherOrderService proxy = (IVoucherOrderService)AopContext.currentProxy();
//            return proxy.createVoucherOrder(voucherId);
//        } finally {
//            lock.unlock();
//        }
//
//    }
    @Transactional
    public void createVoucherOrder(VoucherOrder voucherOrder) {

        //一人只买一单
        Long userId = voucherOrder.getUserId();
        int count = query().eq("user_id", userId).eq("voucher_id", voucherOrder.getVoucherId()).count();
        if (count > 0) {
            log.error("已购买过一次！");
            //return Result.fail("已购买过一次！");
        }

        //全部通过，开始下单扣库存
        boolean success = seckillVoucherService.update()
                .setSql("stock = stock - 1")
                .gt("stock", 0)
                .eq("voucher_id", voucherOrder.getVoucherId()).update();

        if (!success) {
            log.error("库存不足！");
            //return Result.fail("库存不足！");
        }
        save(voucherOrder);

    }

}

