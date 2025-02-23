package com.alibaba.datax.plugin.reader.kafkareader;


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
    public static final String COLUMNS = "columns"; // 收到消息的属性列表，其中第一个为partitionkey
    public static final String ACK = "ack";

    public static final String BATCH_SIZE = "batchSize";

    public static final String RETRIES = "retries";
    public static final String GROUP_ID = "groupId";

    public static final String KEYSERIALIZER = "keySerializer";

    public static final String VALUESERIALIZER = "valueSerializer";

    // not must , not default
    public static final String FIELD_DELIMITER = "fieldDelimiter";
    public static final String COLUMN_COUNT = "columnCount";

    public static final String NO_TOPIC_CREATE = "noTopicCreate";

    public static final String TOPIC_NUM_PARTITION = "topicNumPartition";

    public static final String TOPIC_REPLICATION_FACTOR = "topicReplicationFactor";

    public static final String WRITE_TYPE= "writeType";
    public static final String WRITE_ORDER= "writeOrder";

    public static final String FROM_TIME = "fromTime";
    public static final String END_TIME = "endTime";

}

