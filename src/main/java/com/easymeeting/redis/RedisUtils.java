package com.easymeeting.redis;

import com.easymeeting.entity.constants.Constants;
import com.easymeeting.entity.dto.MeetingMemberDto;
import com.easymeeting.entity.dto.TokenUserInfoDto;
import com.easymeeting.entity.enums.MeetingMemberStatusEnum;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;


@Component("redisUtils")
public class RedisUtils {
    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    public void setRedisTemplate(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * 指定缓存的失效时间
     *
     * @param key  redis中的key
     * @param time 设置过期时间类型为 秒
     * @return 该key对应的值是否失效 true为没有失效；false为失效
     */
    public boolean setExpire(String key, long time) {
        try {
            if (time > 0) {
                redisTemplate.expire(key, time, TimeUnit.SECONDS);
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 根据key拿到key的失效时间
     *
     * @param key
     * @return 失效时间  秒  就是距离过期时间有多少秒
     */
    public long getExpire(String key) {
        return redisTemplate.getExpire(key, TimeUnit.SECONDS);
    }

    /**
     * 判断Key是否在redis中存在
     *
     * @param key 是
     * @return true存在，否则false
     */
    public boolean hasKey(String key) {
        return redisTemplate.hasKey(key);
    }

    /**
     * 删除缓存中所有的keys的值
     *
     * @param keys 可变长度的参数，可以是0个，1个和多个key进行删除
     */
    public void delete(String... keys) {
        if (keys != null && keys.length > 0) {
            if (keys.length == 1) {
                redisTemplate.delete(keys[0]);
            }
        } else {
            redisTemplate.delete(String.valueOf(CollectionUtils.arrayToList(keys)));
        }
    }


    //String类型/

    /**
     * 获取String类型的key对应的值
     *
     * @param key String类型redis数据的key
     * @return 该key对应的String的value值
     */
    public Object get(String key) {
        return key == null ? null : redisTemplate.opsForValue().get(key);
    }

    /**
     * 在redis服务器中是设置String类型的值
     *
     * @param key   String 类型的key
     * @param value 值
     * @return true 添加成功  否则 false
     */
    public boolean set(String key, Object value) {
        try {
            redisTemplate.opsForValue().set(key, value);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 在redis服务器中设置String类型的值，并设置失效时间
     * @param key String 类型的key
     * @param value 值
     * @param time 失效时间 秒
     * @return 设置成功返回true  否则false
     */
    public boolean set(String key, Object value, Long time) {
        try {
            if (time > 0) {
                redisTemplate.opsForValue().set(key, value, time, TimeUnit.SECONDS);
            } else {
                set(key,value);
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }


    /**
     * list集合
     * @param key
     * @param values
     * @return
     */
    public boolean listRightPush(String key, Object... values){
        Long pushAll = redisTemplate.opsForList().rightPushAll(key, values);
        if (pushAll > 0 ){
            return true;
        }

        return false;
    }

    /**
     * redis集合的获取
     * @param key
     * @return
     */
    public List<Object> rangeList(String key){
        List<Object> list = redisTemplate.opsForList().range(key, 0, -1);
        return list;
    }

    public List<Object> hget(String key) {
        try {
            return redisTemplate.opsForHash().values(key);
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }




    /**
     * hash类型数据的存储
     * @param key hash 类型值的key
     * @param map 键值对
     * @return
     */
    public boolean hmset(String key, Map<String,Object> map){
        try {
            redisTemplate.opsForHash().putAll(key,map);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * hash 类型数据存储
     * @param key hash类型的Key
     * @param map 键值对
     * @param time 失效时间
     * @return true设置成功，否则false
     */
    public boolean hmset(String key,Map<String,Object> map, long time){
        try {
            redisTemplate.opsForHash().putAll(key,map);
            if (time > 0){
                setExpire(key,time);
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }



    /**
     * 设置hash中指定key下的field的值为value
     * @param key  hash 的key建
     * @param field hash中的field域
     * @param value 给hash中的field设置的值
     * @return true设置成功，否则false
     */
    public boolean hset(String key,String field, Object value){
        try {
            redisTemplate.opsForHash().put(key,field,value);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public void hdel(String key, String... fields) {
        redisTemplate.opsForHash().delete(key, (Object[]) fields);
    }
    /**
     * 设置hash中指定key下field的值为value并设置失效时间
     * @param key hash的key
     * @param field hash的fieid
     * @param value 给hash中的key下的fieid 设置的值
     * @param time 失效时间
     * @return true设置成功 否则false
     */
    public boolean hset(String key,String field,Object value, long time){
        try {
            redisTemplate.opsForHash().put(key,field,value);
            if (time > 0){
                setExpire(key,time);
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }


    /**
     * 获取hash类型数据的key对应的整个map对象
     * @param key hash 中的Key
     * @param field key对应的hash对象
     * @return 该hash key 对应的hash对应
     */
    public Object hget(String key,String field){
        return redisTemplate.opsForHash().get(key,field);
    }


    /**
     * 获取hash类型数据的key对应的整个map对象
     * @param key hash 中的key
     * @return 该hash key对应的hash对象
     */
    public Map<Object,Object> hmget(String key){
        return redisTemplate.opsForHash().entries(key);
    }

    public void saveTokenUserInfoDto(TokenUserInfoDto tokenUserInfoDto) {
        this.set(Constants.REDIS_KEY_WS_TOKEN + tokenUserInfoDto.getToken() , tokenUserInfoDto , Constants.REDIS_KEY_EXPIRE_DAY);
        this.set(Constants.REDIS_KEY_WS_TOKEN_USERID + tokenUserInfoDto.getUserId(), tokenUserInfoDto.getToken() , Constants.REDIS_KEY_EXPIRE_DAY );
    }


    public TokenUserInfoDto getTokenUserInfoDto(String token) {
        return (TokenUserInfoDto) this.get(Constants.REDIS_KEY_WS_TOKEN + token);
    }

    public TokenUserInfoDto getTokenUserInfoDtoByUserId(String userId) {
        return getTokenUserInfoDto((String) this.get(Constants.REDIS_KEY_WS_TOKEN_USERID + userId));
    }

    public void add2Meeting(String meetingId, MeetingMemberDto meetingMemberDto) {
        this.hset(Constants.REDIS_KEY_MEETING_ROOM + meetingId , meetingMemberDto.getUserId() , meetingMemberDto);
    }

    /**
     * 获取当前会议号中的所有成员
     * @param meetingId
     * @return
     */
    public List<MeetingMemberDto> getMeetingMembers(String meetingId) {
        List<Object> rawList = this.hget(Constants.REDIS_KEY_MEETING_ROOM + meetingId);
        List<MeetingMemberDto> members = rawList.stream()
                .map(obj -> (MeetingMemberDto) obj)
                .collect(Collectors.toList());
        members = members.stream().sorted(Comparator.comparing(MeetingMemberDto::getJoinTime)).collect(Collectors.toList());
        return members;
    }

    public MeetingMemberDto getMeetingMemberDto(String meetingId , String userId) {
        return (MeetingMemberDto) this.hget(Constants.REDIS_KEY_MEETING_ROOM + meetingId , userId);
    }

    public Boolean exitMeeting(String meetingId, String userId, MeetingMemberStatusEnum memberStatusEnum) {
        MeetingMemberDto memberDto = this.getMeetingMemberDto(meetingId , userId);
        if(memberDto == null){
            return false;
        }

        memberDto.setStatus(memberStatusEnum.getStatus());//设置为退出会议
        add2Meeting(meetingId , memberDto ); //更新redis中的信息
        return true;
    }


    public void removeAllMeetingMembers(String meetingId) {
        List<MeetingMemberDto> meetingMemberDtoList = this.getMeetingMembers(meetingId);
        List<String> userIdList = meetingMemberDtoList.stream().map(MeetingMemberDto::getUserId).collect(Collectors.toList());
        if(userIdList == null) {
            return ;
        }
        this.hdel(Constants.REDIS_KEY_MEETING_ROOM + meetingId , userIdList.toArray(new String[userIdList.size()]));
    }

    public Long incr(String key) {
        return redisTemplate.opsForValue().increment(key);
    }

    public void expire(String key, int seconds) {
        redisTemplate.expire(key, seconds, TimeUnit.SECONDS);
    }

}
