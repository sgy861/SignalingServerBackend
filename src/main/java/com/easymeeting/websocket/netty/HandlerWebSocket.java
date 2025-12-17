package com.easymeeting.websocket.netty;

import com.easymeeting.entity.dto.MessageSendDto;
import com.easymeeting.entity.dto.PeerConnectionDataDto;
import com.easymeeting.entity.dto.PeerMessageDto;
import com.easymeeting.entity.dto.TokenUserInfoDto;
import com.easymeeting.entity.enums.MessageSend2TypeEnum;
import com.easymeeting.entity.enums.MessageTypeEnum;
import com.easymeeting.entity.po.UserInfo;
import com.easymeeting.entity.query.UserInfoQuery;
import com.easymeeting.mappers.UserInfoMapper;
import com.easymeeting.redis.RedisUtils;
import com.easymeeting.service.impl.UserInfoServiceImpl;
import com.easymeeting.utils.JsonUtils;
import com.easymeeting.websocket.message.MessageHandler;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Component
@ChannelHandler.Sharable
@Slf4j
public class HandlerWebSocket extends SimpleChannelInboundHandler<TextWebSocketFrame> {

    @Resource
    private UserInfoMapper<UserInfo, UserInfoQuery> userInfoMapper;
    @Resource
    private UserInfoServiceImpl userInfoService;
    @Resource
    private RedisUtils redisUtils;
    @Resource
    private MessageHandler messageHandler;

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        log.info("新用户连接..");
    }

    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        log.info("有连接已经断开...");
        //TODO:处理断开链接的逻辑
        Attribute<String> attribute = ctx.channel().attr(AttributeKey.valueOf(ctx.channel().id().toString()));
        String userId = attribute.get();
        UserInfo userInfo = new UserInfo();
        userInfo.setLastOffTime(System.currentTimeMillis());
        userInfoMapper.updateByUserId(userInfo , userId);
    }

    /**
     * 消息转发，收到前端的消息，转发给其他的客户端
     * @param channelHandlerContext
     * @param textWebSocketFrame
     * @throws Exception
     */
    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext,
                                TextWebSocketFrame textWebSocketFrame)
            throws Exception {
        String text = textWebSocketFrame.text();
        log.error("收到消息:{}" , text);

        PeerConnectionDataDto dataDto = JsonUtils.covertJson2Obj(text, PeerConnectionDataDto.class);

        TokenUserInfoDto tokenUserInfoDto = redisUtils.getTokenUserInfoDto(dataDto.getToken());
        if(null == tokenUserInfoDto){
            return ;
        }

        MessageSendDto messageSendDto = new MessageSendDto();
        messageSendDto.setMessageType(MessageTypeEnum.PEER.getType());

        PeerMessageDto peerMessageDto = new PeerMessageDto();
        peerMessageDto.setSignalData(dataDto.getSignalData());
        peerMessageDto.setSignalType(dataDto.getSignalType());

        messageSendDto.setMessageContent(peerMessageDto);
        messageSendDto.setMeetingId(tokenUserInfoDto.getCurrentMeetingId());
        messageSendDto.setSendUserId(tokenUserInfoDto.getUserId());
        messageSendDto.setReceiveUserId(dataDto.getReceiveUserId());

        messageSendDto.setMessageSend2Type(MessageSend2TypeEnum.USER.getType());

        messageHandler.sendMessage(messageSendDto);
    }
}
