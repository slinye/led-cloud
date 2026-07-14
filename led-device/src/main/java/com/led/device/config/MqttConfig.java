package com.led.device.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.mqtt.core.DefaultMqttPahoClientFactory;
import org.springframework.integration.mqtt.core.MqttPahoClientFactory;
import org.springframework.integration.mqtt.inbound.MqttPahoMessageDrivenChannelAdapter;
import org.springframework.integration.mqtt.outbound.MqttPahoMessageHandler;
import org.springframework.integration.mqtt.support.DefaultPahoMessageConverter;
import org.springframework.messaging.MessageChannel;

@Configuration
public class MqttConfig {

    @Value("${mqtt.broker-url}")
    private String brokerUrl;
    @Value("${mqtt.client-id}")
    private String clientId;
    @Value("${mqtt.username}")
    private String username;
    @Value("${mqtt.password}")
    private String password;
    @Value("${mqtt.topic-prefix}")
    private String topicPrefix;

    @Bean
    public MqttPahoClientFactory mqttClientFactory() {
        DefaultMqttPahoClientFactory factory = new DefaultMqttPahoClientFactory();
        factory.getConnectionOptions().setServerURIs(new String[]{brokerUrl});
        factory.getConnectionOptions().setUserName(username);
        factory.getConnectionOptions().setPassword(password.toCharArray());
        factory.getConnectionOptions().setCleanSession(true);
        factory.getConnectionOptions().setAutomaticReconnect(true);
        factory.getConnectionOptions().setKeepAliveInterval(30);
        return factory;
    }

    @Bean
    public MessageChannel mqttOutboundChannel() {
        return new DirectChannel();
    }

    @Bean
    public MqttPahoMessageHandler mqttOutbound() {
        MqttPahoMessageHandler handler = new MqttPahoMessageHandler(clientId + "-out", mqttClientFactory());
        handler.setAsync(true);
        handler.setDefaultTopic(topicPrefix + "/command/#");
        return handler;
    }

    @Bean
    public MessageChannel mqttInboundChannel() {
        return new DirectChannel();
    }

    @Bean
    public MqttPahoMessageDrivenChannelAdapter mqttInbound() {
        MqttPahoMessageDrivenChannelAdapter adapter = new MqttPahoMessageDrivenChannelAdapter(
                clientId + "-in", mqttClientFactory(),
                topicPrefix + "/status/#",
                topicPrefix + "/heartbeat/#",
                topicPrefix + "/register/#");
        adapter.setConverter(new DefaultPahoMessageConverter());
        adapter.setQos(1);
        adapter.setOutputChannel(mqttInboundChannel());
        return adapter;
    }
}
