package com.easymeeting.aspect;

import com.easymeeting.annotation.RequestLimit;
import com.easymeeting.entity.dto.TokenUserInfoDto;
import com.easymeeting.exception.BusinessException;
import com.easymeeting.redis.RedisUtils;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;

@Aspect
@Component
public class RequestLimitAspect {

    @Resource
    private RedisUtils redisUtils;

    @Before("@annotation(com.easymeeting.annotation.RequestLimit)")
    public void beforeRequestLimit(JoinPoint joinPoint) {
        Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
        RequestLimit limit = method.getAnnotation(RequestLimit.class);
        if (limit == null) {
            return;
        }

        int timeWindow = limit.timeWindow();  // 时间窗口（秒）
        int maxCount = limit.maxCount();      // 最大次数

        // 1. 获取当前请求
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = attrs.getRequest();

        // 2. 构造限流 key（按用户 or 按 IP）
        String key = buildLimitKey(request, limit);

        // 3. 使用 Redis 进行计数
        long count = incrementRequestCount(key, timeWindow);

        // 4. 判断是否超限
        if (count > maxCount) {
            throw new BusinessException("请求过于频繁，请稍后再试");
        }
    }

    private String buildLimitKey(HttpServletRequest request, RequestLimit limit) {
        StringBuilder key = new StringBuilder("req_limit:");

        if (limit.byUser()) {
            // 这里你可以结合你的 TokenUserInfoDto
            String token = request.getHeader("token");
            // 简单写法，未登录用户可以按 IP 限制
            String userId = "anonymous";

            TokenUserInfoDto userInfo = redisUtils.getTokenUserInfoDto(token);
            if (userInfo != null) { userId = userInfo.getUserId(); }

            key.append("USER:").append(userId);
        } else {
            String ip = request.getRemoteAddr();
            key.append("IP:").append(ip);
        }

        // 加上方法名，做到“按接口限流”
        key.append(":").append(request.getRequestURI());

        return key.toString();
    }

    private long incrementRequestCount(String key, int timeWindowSeconds) {
        // 伪代码：结合你自己的 RedisUtils 做封装
        // 一般实现思路是：
        // 1. INCR key
        // 2. 如果返回值 == 1，则顺便设置 EXPIRE timeWindow
        Long count = redisUtils.incr(key);
        if (count == 1L) {
            redisUtils.expire(key, timeWindowSeconds);
        }
        return count;
    }
}
