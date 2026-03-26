package com.yuki.webapp.service.impl;

import com.yuki.webapp.config.VerificationCodeConfig;
import com.yuki.webapp.service.VerificationCodeService;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Service
public class VerificationCodeServiceImpl implements VerificationCodeService{
//    @Autowired
//    private RedisTemplate<String, String> redisTemplate;
//
//    @Override
//    public String generateVerificationCode(String email) {
//        String code = RandomStringUtils.randomNumeric(6); // 生成6位数字验证码
//        redisTemplate.opsForValue().set(email, code, 5, TimeUnit.MINUTES); // 存储5分钟
//        return code;
//    }
//    @Override
//    public boolean verifyCode(String email, String code) {
//        String storedCode = redisTemplate.opsForValue().get(email);
//        return code.equals(storedCode);
//    }

// 使用 ConcurrentHashMap 存储邮箱和验证码的映射关系
    @Autowired
    private VerificationCodeConfig verificationCodeConfig;
    private final Map<String, String> codeMap = new ConcurrentHashMap<>();
    // 使用 ConcurrentHashMap 存储邮箱和验证码生成时间的映射关系

    private final Map<String, Long> codeTimeMap = new ConcurrentHashMap<>();
    // 5分钟

    // 创建一个定时任务调度器，用于定期清理过期的验证码
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    // 安排一个定时任务，每隔 1 分钟执行一次 cleanExpiredCodes 方法
    public VerificationCodeServiceImpl() {
        scheduler.scheduleAtFixedRate(this::cleanExpiredCodes, 1, 1, TimeUnit.MINUTES);
    }

    public String generateVerificationCode(String email) {
        // 生成一个 6 位随机数作为验证码
        String code = String.valueOf((int) (Math.random() * 900000 + 100000));
        codeMap.put(email, code);
        codeTimeMap.put(email, System.currentTimeMillis());
        return code;
    }

    public boolean verifyCode(String email, String code) {

        // 从 codeMap 中获取该邮箱对应的验证码
        String storedCode = codeMap.get(email);
        if (storedCode == null || !storedCode.equals(code)) {
            return false;
        }

        // 从 codeTimeMap 中获取验证码的生成时间
        Long generateTime = codeTimeMap.get(email);
        if (generateTime == null || System.currentTimeMillis() - generateTime > verificationCodeConfig.CODE_EXPIRE_TIME) {

            // 清理过期的验证码
            codeMap.remove(email);
            codeTimeMap.remove(email);
            return false;
        }

        codeMap.remove(email);
        codeTimeMap.remove(email);
        return true;
    }

    /**
     * 清理过期的验证码
     */
    private void cleanExpiredCodes() {
        long currentTime = System.currentTimeMillis();
        codeTimeMap.entrySet().removeIf(entry -> {
            if (currentTime - entry.getValue() > verificationCodeConfig.CODE_EXPIRE_TIME) {
                codeMap.remove(entry.getKey());
                return true;
            }
            return false;
        });
    }
}
