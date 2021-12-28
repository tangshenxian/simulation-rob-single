package com.shenxian.single.controller;

import com.shenxian.single.utils.JedisUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * @author: shenxian
 * @date: 2021/12/28 10:16
 */
@Slf4j
@RestController
public class JedisController {

    @Autowired
    private JedisUtil jedisUtil;

    @GetMapping("/setNx/{key}/{value}")
    public boolean setNx(@PathVariable String key, @PathVariable String value) {
        return jedisUtil.setNx(key, value);
    }

    @GetMapping("/delNx/{key}/{value}")
    public int delNx(@PathVariable String key, @PathVariable String value) {
        return jedisUtil.delNx(key, value);
    }

    // 总库存
    private long kuCun = 0;

    // 商品key
    private String SHANG_PIN_KEY = "SHANG_PIN_KEY";

    // 获取锁的超时时间30秒
    private int timeout = 30 * 1000;

    @GetMapping("/qiangDan")
    public List<String> qiangDan() {
        // 抢到商品的用户
        List<String> successUsers = new ArrayList<>();

        // 构造10W用户
        List<String> users = new ArrayList<>();
        IntStream.range(0, 100000).forEach(o -> {
            users.add("用户-" + o);
        });

        // 初始化库存
        kuCun = 10;

        // 抢单开始
        users.parallelStream().forEach(o -> {
            String successUser = start(o);
            if (!StringUtils.isEmpty(successUser)) {
                successUsers.add(successUser);
            }
        });
        return successUsers;
    }

    /**
     * 模拟抢单动作
     * @param user
     * @return
     */
    private String start(String user) {
        // 开始抢单时间
        long start = System.currentTimeMillis();

        // 未抢到的情况下，30秒内持续获取锁
        while ((start + timeout) >= System.currentTimeMillis()) {
            //判断库存是否剩余
            if (kuCun <= 0) {
                break;
            }
            // 用户拿到锁
            if (jedisUtil.setNx(SHANG_PIN_KEY, user)) {
                try {
                    log.info("{}拿到锁", user);
                    // 模拟生成订单的耗时
                    try {
                        TimeUnit.SECONDS.sleep(1);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    // 抢单成功，库存-1
                    kuCun -= 1;
                    log.error("{}抢单成功， 库存剩余{}", user, kuCun);
                    return user + "抢单成功，剩余库存：" + kuCun;
                } finally {
                    log.info("{}释放锁", user);
                    jedisUtil.delNx(SHANG_PIN_KEY, user);
                }
            }
        }
        return "";
    }

}
