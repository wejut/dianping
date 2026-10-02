package com.hmdp;

import com.hmdp.utils.RedisIdWorker;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


@SpringBootTest
class HmDianPingApplicationTests {

    @Autowired
    private RedisIdWorker redisIdWorker;

    private ExecutorService executorService = Executors.newFixedThreadPool(500);
    
    public void testRedisIdWorker() {
        Runnable task = () -> {
            long id = redisIdWorker.nextId("order");
            System.out.println("id = " + id);
        };
        for (int i = 0; i < 500; i++) {
            executorService.submit(task);
        }
    }
}
