package com.easymeeting.websocket.message;

import com.easymeeting.entity.constants.Constants;
import com.easymeeting.entity.dto.MessageSendDto;
import com.easymeeting.utils.JsonUtils;
import com.easymeeting.websocket.ChannelContextUtils;
import com.rabbitmq.client.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;


@Slf4j
@Component
@ConditionalOnProperty(name = Constants.MESSAGE_HANDLE_CHANNEL_KEY , havingValue = Constants.MESSAGE_HANDLE_CHANNEL_RABBITMQ)
public class MessageHandler4RabbitMq implements MessageHandler {

    private static final String EXCHANGE_NAME = "fanout_exchange";

    private static final Integer MAX_RETRY_COUNT = 3;
    private static final String RETRY_COUNT_KEY = "retryCount";

    @Resource
    private ChannelContextUtils channelContextUtils;

    @Value("${rabbitmq.host}")
    private String rabbitmqHost;


    @Value("${rabbitmq.port}")
    private Integer rabbitmqPort;


    private ConnectionFactory factory;
    private Connection connection;
    private Channel channel;

    @Override
    public void listenMessage() {
        factory = new ConnectionFactory();
        factory.setHost(rabbitmqHost);
        factory.setPort(rabbitmqPort);
        try {
            connection = factory.newConnection();
            channel = connection.createChannel();
            channel.exchangeDeclare(EXCHANGE_NAME, BuiltinExchangeType.FANOUT);
            String queueName = channel.queueDeclare().getQueue();
            channel.queueBind(queueName, EXCHANGE_NAME, "");

            Boolean autoAck = false;

            DeliverCallback deliverCallback = (consumerTag, delivery) -> {
                try {
                    String message = new String(delivery.getBody(), "UTF-8");
                    log.info("Message received: {}", message);

                    channelContextUtils.sendMessage(JsonUtils.covertJson2Obj(message, MessageSendDto.class));
                    channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
                }catch (Exception e) {
                    log.info("消息处理失败" , e);
                    handleFailMessage(channel , delivery , queueName);
                }
            };

            channel.basicConsume(queueName, autoAck, deliverCallback, consumerTag -> {

            });
        }catch (Exception e) {
            log.error("rabbitMq监听消息失败");
        }
    }

    private static void handleFailMessage(Channel channel, Delivery  delivery , String queueName) throws IOException {
        Map<String , Object> headers = delivery.getProperties().getHeaders();
        if(null == headers) {
            headers = new HashMap<>();
        }
        Integer retryCount = 0;
        if(headers.containsKey(RETRY_COUNT_KEY)) {
            retryCount = (Integer)headers.get(RETRY_COUNT_KEY);
        }

        //重发消息
        if(retryCount < MAX_RETRY_COUNT - 1) {
            //设置重发次数
            headers.put(RETRY_COUNT_KEY, retryCount + 1);
            //构造新的消息(带上新设置的重发次数)
            AMQP.BasicProperties properties = new AMQP.BasicProperties.Builder().headers(headers).build();
            //重新发送信息到队列
            channel.basicPublish("", queueName, properties, delivery.getBody());
            //ack原消息，避免积压
            channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
        } else {
            log.info("超过最大重试次数");
            channel.basicReject(delivery.getEnvelope().getDeliveryTag(), false);
        }

    }

    @Override
    public void sendMessage(MessageSendDto messageSendDto) {
        try(Connection connection = factory.newConnection();
            Channel channel = connection.createChannel()) {
            channel.exchangeDeclare(EXCHANGE_NAME, BuiltinExchangeType.FANOUT);
            String message = "这是我发布的一条消息(" + System.currentTimeMillis() + ")";
            channel.basicPublish(EXCHANGE_NAME, "", null, message.getBytes());
        }catch (Exception e) {
            log.error("rabbitMq发送消息失败");
        }
    }

    @PreDestroy
    private void destroy() throws IOException, TimeoutException {
        if(channel != null && channel.isOpen()) {
            channel.close();
        }
        if(connection != null && connection.isOpen()) {
            connection.close();
        }
    }
}
