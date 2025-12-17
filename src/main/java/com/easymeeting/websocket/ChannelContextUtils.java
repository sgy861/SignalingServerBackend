package com.easymeeting.websocket;

import com.alibaba.fastjson.JSON;
import com.easymeeting.entity.dto.MeetingExitDto;
import com.easymeeting.entity.dto.MeetingMemberDto;
import com.easymeeting.entity.dto.MessageSendDto;
import com.easymeeting.entity.dto.TokenUserInfoDto;
import com.easymeeting.entity.enums.MeetingMemberStatusEnum;
import com.easymeeting.entity.enums.MessageSend2TypeEnum;
import com.easymeeting.entity.enums.MessageTypeEnum;
import com.easymeeting.entity.po.UserInfo;
import com.easymeeting.mappers.UserInfoMapper;
import com.easymeeting.redis.RedisUtils;
import com.easymeeting.utils.JsonUtils;
import com.easymeeting.utils.StringTools;
import io.netty.channel.Channel;
import io.netty.channel.ChannelException;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.util.AttributeKey;
import io.netty.util.concurrent.GlobalEventExecutor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

;
import javax.annotation.Resource;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Component
@Slf4j
public class ChannelContextUtils {
    /**
     * userId -> channel
     */
    public static final ConcurrentHashMap<String , Channel> USER_CONTEXT_MAP = new ConcurrentHashMap<>();

    /**
     * meetingId -> channelGroup
     */
    public static final ConcurrentHashMap<String , ChannelGroup> MEETING_ROOM_CONTEXT_MAP = new ConcurrentHashMap<>();
    @Resource
    private  UserInfoMapper userInfoMapper;
    @Resource
    private RedisUtils redisUtils;

    /**
     * 绑定userId和对应的channel
     * @param userId
     * @param channel
     */
    public void addContext(String userId , Channel channel) {
        try{
            String channelId = channel.id().toString();
            AttributeKey attributeKey = null;
            if (!AttributeKey.exists(channelId)) {
                attributeKey = AttributeKey.newInstance(channelId);
            }else {
                attributeKey = AttributeKey.valueOf(channelId);
            }
            channel.attr(attributeKey).set(userId); //Netty的上下文信息，设置userId存放在对对应的attributeKey中
            USER_CONTEXT_MAP.put(userId , channel);
            UserInfo userInfo = new UserInfo();
            userInfo.setLastLoginTime(System.currentTimeMillis());
            userInfoMapper.updateByUserId(userInfo , userId);
            TokenUserInfoDto tokenUserInfoDto = redisUtils.getTokenUserInfoDtoByUserId(userId);
            if(tokenUserInfoDto.getCurrentMeetingId() == null){
                return ;
            }
            addMeetingRoom(tokenUserInfoDto.getCurrentMeetingId(), userId);

        }catch (Exception e) {
            log.error("初始化连接失败" , e);
        }
    }
    public void addMeetingRoom(String meetingId , String userId) {
        Channel context = USER_CONTEXT_MAP.get(userId);

        if(context == null){
            log.info("还没加入进ws房间中");
            return ;
        }
        log.info("已经加入到ws房间中");
        ChannelGroup group = MEETING_ROOM_CONTEXT_MAP.get(meetingId);
        if(group == null){
            group = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
            MEETING_ROOM_CONTEXT_MAP.put(meetingId , group);
        }
        Channel channel = group.find(context.id());
        if(channel == null){
            group.add(context);
        }

    }

    public void sendMessage(MessageSendDto messageSendDto) {
         if(MessageSend2TypeEnum.USER.getType().equals(messageSendDto.getMessageSend2Type())){
             sendMsg2User(messageSendDto);
         }else {
             sendMsg2Group(messageSendDto);
         }
    }
    private void sendMsg2Group(MessageSendDto messageSendDto){
        if(messageSendDto.getMeetingId() == null){
            return;
        }
        ChannelGroup channelGroup = MEETING_ROOM_CONTEXT_MAP.get(messageSendDto.getMeetingId());
        if(channelGroup == null){
            return ;
        }
        log.info("加入的用户信息为：{}" , messageSendDto.toString());
        channelGroup.writeAndFlush(new TextWebSocketFrame(JSON.toJSONString(messageSendDto)));

        //当信息类型为退出房间的时候，需要把channel删掉
        if(MessageTypeEnum.EXIT_MEETING_ROOM.getType().equals(messageSendDto.getMessageSend2Type())){
            MeetingExitDto meetingExitDto = JsonUtils.covertJson2Obj((String)messageSendDto.getMessageContent() ,
                    MeetingExitDto.class);
            removeContextFromGroup(meetingExitDto.getExitUserId() , messageSendDto.getMeetingId());
            List<MeetingMemberDto> meetingMemberDtoList = redisUtils.getMeetingMembers(messageSendDto.getMeetingId());
            List<MeetingMemberDto> onLineMemberList = meetingMemberDtoList.stream().filter(item ->
                    MeetingMemberStatusEnum.NORMAL.getStatus().equals(item.getStatus())).collect(Collectors.toList());

            if(onLineMemberList.isEmpty()) {
                removeContextGroup(messageSendDto.getMeetingId());
            }

            return ;
        }
        //类型为结束会议的时候，删除所有的channel
        if(MessageTypeEnum.FINISH_MEETING.getType().equals(messageSendDto.getMessageSend2Type())){
            List<MeetingMemberDto> meetingMemberDtoList = redisUtils.getMeetingMembers(messageSendDto.getMeetingId());
            for(MeetingMemberDto meetingMemberDto : meetingMemberDtoList){
                removeContextFromGroup(meetingMemberDto.getUserId() , messageSendDto.getMeetingId());
            }

            removeContextGroup(messageSendDto.getMeetingId());
        }
    }

    private void removeContextGroup(String meetingId) {
        MEETING_ROOM_CONTEXT_MAP.remove(meetingId);
    }
    private void removeContextFromGroup(String userId , String meetingId) {
        Channel context = USER_CONTEXT_MAP.get(userId);
        if(null == context) {
            return ;
        }
        ChannelGroup group = MEETING_ROOM_CONTEXT_MAP.get(meetingId);
        if(group != null ) {
            group.remove(context);
        }
    }

    private void sendMsg2User(MessageSendDto messageSendDto){

        if(messageSendDto.getReceiveUserId() == null){
            return ;
        }
        Channel  channel = USER_CONTEXT_MAP.get(messageSendDto.getReceiveUserId());
        if(channel == null){
            return ;
        }

        channel.writeAndFlush(new TextWebSocketFrame(JSON.toJSONString(messageSendDto)));
    }


    public void closeContext(String userId){
        if(StringTools.isEmpty(userId)){
            return ;
        }
        Channel channel = USER_CONTEXT_MAP.get(userId);
        USER_CONTEXT_MAP.remove(userId);
        if(channel != null){
            channel.close();
        }

    }
}
