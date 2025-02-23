package com.alibaba.datax.plugin.writer.kafkawriter;


/**
 * @ClassName: Key
 * @Author: majun
 * @CreateDate: 2019/2/20 11:17
 * @Version: 1.0
 * @Description: TODO
 */

public class Key {
//
//    bootstrapServers": "",
//            "topic": "",
//            "ack": "all",
//            "batchSize": 1000,
//            "retries": 0,
//            "keySerializer":"org.apache.kafka.common.serialization.StringSerializer",
//            "valueSerializer": "org.apache.kafka.common.serialization.StringSerializer",
//            "fieldFelimiter": ","

    public static final String BOOTSTRAP_SERVERS="bootstrapServers";

    // must have
    public static final String TOPIC = "topic";

    public static final String ACK = "ack";

    public static final String BATCH_SIZE = "batchSize";

    public static final String RETRIES = "retries";

    public static final String KEYSERIALIZER = "keySerializer";

    public static final String VALUESERIALIZER = "valueSerializer";
    public static final String COLUMNS = "columns";
    // not must , not default
    public static final String NO_TOPIC_CREATE = "noTopicCreate";

    public static final String TOPIC_NUM_PARTITION = "topicNumPartition";

    public static final String TOPIC_REPLICATION_FACTOR = "topicReplicationFactor";

    public static final String WRITE_TYPE= "writeType";
}

