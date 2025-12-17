package com.easymeeting.websocket.test;

import com.rabbitmq.client.*;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;

@Slf4j
public class RabbitMqSubscriber {
    private static final String EXCHANGE_NAME = "fanout_exchange";

    private static final Integer MAX_RETRY_COUNT = 3;
    private static final String RETRY_COUNT_KEY = "retryCount";
    public static void main(String[] args) throws IOException, TimeoutException {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        factory.setPort(5672);
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();
        channel.exchangeDeclare(EXCHANGE_NAME, BuiltinExchangeType.FANOUT);
        String queueName = channel.queueDeclare().getQueue();
        channel.queueBind(queueName, EXCHANGE_NAME, "");

        Boolean autoAck = false;

        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
            try {
                String message = new String(delivery.getBody(), "UTF-8");
                log.info("收到消息: {}", message + System.currentTimeMillis());
                if(Math.random() > 0.5){
                    throw new RuntimeException("模拟处理失败");
                }
                channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
            }catch (Exception e) {
                log.info("消息处理失败" , e);
                try {
                    handleFailMessage(channel , delivery , queueName);
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            }
        };

        channel.basicConsume(queueName, autoAck, deliverCallback, consumerTag -> {
        });
        log.info("订阅已启动，等待消息中...");
    }

    private static void handleFailMessage(Channel channel, Delivery  delivery , String queueName) throws Exception {
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
}

