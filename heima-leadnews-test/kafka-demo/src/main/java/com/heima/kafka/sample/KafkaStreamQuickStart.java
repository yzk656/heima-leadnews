package com.heima.kafka.sample;

import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.TimeWindows;
import org.apache.kafka.streams.kstream.ValueMapper;

import java.time.Duration;
import java.util.Arrays;
import java.util.Properties;

/**
 * 流式处理
 */
public class KafkaStreamQuickStart {
    public static void main(String[] args) {
        //kafka配置信息
        Properties properties = new Properties();
        properties.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG,"192.168.150.101:9092");
        properties.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass());
        properties.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass());
        properties.put(StreamsConfig.APPLICATION_ID_CONFIG,"streams-quickstart");

        //stream构建器
        StreamsBuilder streamsBuilder = new StreamsBuilder();

        //流式计算
        streamProcessor(streamsBuilder);

        //创建kafkaStream流对象
        KafkaStreams kafkaStreams = new KafkaStreams(streamsBuilder.build(),properties);
        //开启流式计算
        kafkaStreams.start();
    }


    private static void streamProcessor(StreamsBuilder streamsBuilder){
        //创建Stream对象，同时指定从哪个topic中接收消息
        KStream<String, String> stream = streamsBuilder.stream("itcast-topic-input");

        /**
         * 处理value
         */
        stream.flatMapValues(new ValueMapper<String, Iterable<?>>() {
            @Override
            public Iterable<?> apply(String value) {
                return Arrays.asList(value.split(" "));
            }
        }).groupBy((key,value)->value)
                //时间窗口
                .windowedBy(TimeWindows.of(Duration.ofSeconds(10)))
                //统计单词个数
                .count()
                //转换为stream
                .toStream()
                .map((key,value)->{
                    System.out.println(key+":"+value);
                    return new KeyValue<>(key.key().toString(),value.toString());
                })
                //发送消息
                .to("itcast-topic-out");
        ;
    }
}
