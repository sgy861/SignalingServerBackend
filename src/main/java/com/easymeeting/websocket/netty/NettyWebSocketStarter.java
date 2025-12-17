package com.easymeeting.websocket.netty;

import com.easymeeting.config.AppConfig;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.sctp.nio.NioSctpServerChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.CharacterEncodingFilter;

import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import java.util.logging.Handler;

@Component
@Slf4j
public class NettyWebSocketStarter implements Runnable {
    /**
     * boos线程，用于处理连接
     */
    private EventLoopGroup bossGroup = new NioEventLoopGroup();

    /**
     * work线程，用于处理消息
     */
    private EventLoopGroup workerGroup = new NioEventLoopGroup();

    @Resource
    private AppConfig appConfig;
    @Resource
    private HandlerWebSocket handlerWebSocket;
    @Resource
    private HandlerTokenValidation handlerTokenValidation;

    @Override
    public void run() {
        try {
            ServerBootstrap serverBootstrap = new ServerBootstrap();
            serverBootstrap.group(bossGroup, workerGroup);
            serverBootstrap.channel(NioServerSocketChannel.class).handler(new LoggingHandler(LogLevel.DEBUG)).
                    childHandler(new ChannelInitializer<Channel>() {
                        @Override
                        protected void initChannel(Channel channel) throws Exception {
                            ChannelPipeline pipeline = channel.pipeline();
                            /**
                             * 对http协议的支持，使用http的编码器和解码器
                             */
                            pipeline.addLast(new HttpServerCodec());
                            /**
                             * http聚合器，将分片的http消息 聚合成完整的FullHTTpRequest
                             */
                            pipeline.addLast(new HttpObjectAggregator(64 * 1024));
                            /**
                             * int readerIdleTimeSeconds 一段时间没有收到客户端消息
                             * int writerIdleTimeSeconds 一段时间没向服务端发消息
                             * int allIdleTimeSeconds 读和写都没活动
                             */
                            pipeline.addLast(new IdleStateHandler(20 , 0,
                                    0));
                            pipeline.addLast(new HandlerHeartBeat());
                            /**
                             * token校验,拦截channelRead事件
                             */
                            pipeline.addLast(handlerTokenValidation);
                            /**
                             * websocket协议处理器
                             * String websocketPath, 路径
                             * String subprotocols, 指定支持的子协议
                             * boolean allowExtensions, 是否允许websocket扩展
                             * int maxFrameSize, 设置最大帧数 6653
                             * boolean allowMaskMismatch,是否允许掩码不匹配
                             * boolean checkStartsWith, 是否严格检查路径开头
                             * long handshakeTimeoutMillis 握手超时时间 mm
                             */
                            pipeline.addLast(new WebSocketServerProtocolHandler("/ws", null,
                                    true, 6553, true, true,
                                    10000L));
                            pipeline.addLast(handlerWebSocket);
                        }
                    });

            Channel channel = serverBootstrap.bind(appConfig.getWsPort()).sync().channel();
            log.info("netty服务启动成功,端口:{}", appConfig.getWsPort());
            channel.closeFuture().sync();
        } catch (Exception e) {
            log.error("Netty启动失败 {}", e.getMessage());

        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }


    @PreDestroy
    public void close() {
        bossGroup.shutdownGracefully();
        workerGroup.shutdownGracefully();
    }
}
