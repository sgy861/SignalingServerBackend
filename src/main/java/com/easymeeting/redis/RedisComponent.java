package com.easymeeting.redis;

import com.easymeeting.entity.constants.Constants;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.UUID;

@Component
public class RedisComponent {

    @Resource
    private RedisUtils redisUtils;

    public String saveCheckCode(String checkCode) {
         String checkCodeKey = UUID.randomUUID().toString();
         redisUtils.set(Constants.REDIS_KEY_CHECK_CODE + checkCodeKey, checkCode ,
                 Constants.REDIS_KEY_EXPIRE_DAY);
         return checkCodeKey;
    }

    public String getCheckCode(String checkCodeKey) {
        return (String) redisUtils.get(Constants.REDIS_KEY_CHECK_CODE + checkCodeKey);
    }

    public void deleteCheckCode(String checkCodeKey) {
        redisUtils.delete(Constants.REDIS_KEY_CHECK_CODE + checkCodeKey);
    }
}
