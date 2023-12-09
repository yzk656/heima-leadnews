package com.heima.kafka.sample;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;

import java.util.Properties;

public class ProducerQuickStart {
    public static void main(String[] args) {
        //kafka链接配置信息
        Properties properties = new Properties();
        //设置kafka链接地址
        properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,"192.168.150.101:9092");
        //key和value的序列化
        properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,"org.apache.kafka.common.serialization.StringSerializer");
        properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,"org.apache.kafka.common.serialization.StringSerializer");

        //创建kafka生产者对象
        KafkaProducer<String,String> kafkaProducer = new KafkaProducer<String,String>(properties);

        //发送消息
        /**
         * topic
         * 消息的key
         * 消息的value
         */
        for (int i = 0; i < 5; i++) {
            ProducerRecord<String, String> producerRecord = new ProducerRecord<>("itcast-topic-input",  "hello kafka");
            kafkaProducer.send(producerRecord);
        }

        //关闭消息通道,必须关闭，否则关闭不成功
        kafkaProducer.close();
    }
}
