package com.easymeeting.websocket.netty;

import com.easymeeting.entity.dto.TokenUserInfoDto;
import com.easymeeting.redis.RedisUtils;
import com.easymeeting.utils.StringTools;
import com.easymeeting.websocket.ChannelContextUtils;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;
import io.netty.util.CharsetUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;

@Component
@Slf4j
@ChannelHandler.Sharable
public class HandlerTokenValidation extends SimpleChannelInboundHandler<FullHttpRequest> {
    @Resource
    private RedisUtils redisUtils;
    @Resource
    private ChannelContextUtils channelContextUtils;

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) throws Exception {
        String uri = request.uri();
        QueryStringDecoder queryStringDecoder = new QueryStringDecoder(uri);
        List<String> tokens = queryStringDecoder.parameters().get("token");
        if(tokens == null || tokens.isEmpty()) {
            this.sendErrorResponse(ctx);
            return ;
        }
        String token = tokens.get(0);
        TokenUserInfoDto tokenUserInfoDto = this.checkToken(token);
        if(tokenUserInfoDto == null) {
            log.error("校验token失败{}" , token);
            this.sendErrorResponse(ctx);
            return ;
        }
        ctx.fireChannelRead(request.retain());
        //TODO:连接成功后初始化的工作
        channelContextUtils.addContext(tokenUserInfoDto.getUserId() , ctx.channel());

    }

    private TokenUserInfoDto checkToken(String token) {
        if(StringTools.isEmpty(token)) {
            return null;
        }
        return  redisUtils.getTokenUserInfoDto(token);
    }

    private void sendErrorResponse(ChannelHandlerContext ctx ){
        FullHttpResponse fullHttpResponse = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.FORBIDDEN ,
                Unpooled.copiedBuffer("token无效" , CharsetUtil.UTF_8));
        fullHttpResponse.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain; charset=utf-8");
        fullHttpResponse.headers().set(HttpHeaderNames.CONTENT_LENGTH, fullHttpResponse.content().readableBytes());
        /**
         * 构造好的响应写入管道并立即发送到客户端
         * 发送后，主动关闭连接
         */
        ctx.writeAndFlush(fullHttpResponse).addListener(ChannelFutureListener.CLOSE);

    }
}
