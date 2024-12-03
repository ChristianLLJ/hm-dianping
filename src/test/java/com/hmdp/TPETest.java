package com.hmdp;

import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.*;

/**
 * Program : hm-dianping
 * Author : llj
 * Create : 2024-07-24 20:08
 **/

public class TPETest {
    ThreadPoolExecutor executor = new ThreadPoolExecutor(2,4,100,TimeUnit.SECONDS,new ArrayBlockingQueue<>(100), new ThreadPoolExecutor.CallerRunsPolicy());
    ExecutorService executorService = Executors.newFixedThreadPool(3);

//    CompletableFuture<String> future = CompletableFuture.completedFuture("hello!")
//            .thenApply(s -> s + "world!");

    CompletableFuture<String> future
            = CompletableFuture.supplyAsync(() -> {
        if (true) {
            throw new RuntimeException("Computation error!");
        }
        return "hello!";
    }).handle((res, ex) -> {
        // res 代表返回的结果
        // ex 的类型为 Throwable ，代表抛出的异常
        return res != null ? res : "world!";
    });

}
