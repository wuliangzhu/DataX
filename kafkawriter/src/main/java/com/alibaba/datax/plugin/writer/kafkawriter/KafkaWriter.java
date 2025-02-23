package com.alibaba.datax.plugin.writer.kafkawriter;

import com.alibaba.datax.common.element.Column;
import com.alibaba.datax.common.element.Record;
import com.alibaba.datax.common.exception.DataXException;
import com.alibaba.datax.common.plugin.RecordReceiver;
import com.alibaba.datax.common.spi.Writer;
import com.alibaba.datax.common.util.Configuration;
import com.alibaba.fastjson.JSON;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.ListTopicsResult;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * @ClassName: KafkaWriter
 * @Author: majun
 * @CreateDate: 2019/2/20 11:06
 * @Version: 1.0
 * @Description: datax kafkawriter
 */

public class KafkaWriter extends Writer {

    public static class Job extends Writer.Job {

        private static final Logger logger = LoggerFactory.getLogger(Job.class);
        private Configuration conf = null;

        @Override
        public List<Configuration> split(int mandatoryNumber) {
            List<Configuration> configurations = new ArrayList<Configuration>(mandatoryNumber);
            for (int i = 0; i < mandatoryNumber; i++) {
                configurations.add(conf);
            }
            return configurations;
        }

        private void validateParameter() {
            this.conf.getNecessaryValue(Key.BOOTSTRAP_SERVERS, KafkaWriterErrorCode.REQUIRED_VALUE);
            this.conf.getNecessaryValue(Key.TOPIC, KafkaWriterErrorCode.REQUIRED_VALUE);
            this.conf.getNecessaryValue(Key.COLUMNS, KafkaWriterErrorCode.REQUIRED_VALUE);
        }

        @Override
        public void init() {
            this.conf = super.getPluginJobConf();
            logger.info("kafka writer params:{}", conf.toJSON());
            this.validateParameter();
        }


        @Override
        public void destroy() {

        }
    }

    public static class Task extends Writer.Task {
        private static final Logger logger = LoggerFactory.getLogger(Task.class);
        private static final String NEWLINE_FLAG = System.getProperty("line.separator", "\n");

        private Producer<String, String> producer;
        private String[] columns;
        private Configuration conf;
        private Properties props;

        @Override
        public void init() {
            this.conf = super.getPluginJobConf();
            String columnsStr = conf.getString(Key.COLUMNS);
            if (columnsStr == null) {

            }
            this.columns = columnsStr.split(",");

            props = new Properties();
            props.put("bootstrap.servers", conf.getString(Key.BOOTSTRAP_SERVERS));
            props.put("acks", conf.getUnnecessaryValue(Key.ACK, "0", null));//这意味着leader需要等待所有备份都成功写入日志，这种策略会保证只要有一个备份存活就不会丢失数据。这是最强的保证。
            props.put("retries", conf.getUnnecessaryValue(Key.RETRIES, "0", null));
            // Controls how much bytes sender would wait to batch up before publishing to Kafka.
            //控制发送者在发布到kafka之前等待批处理的字节数。
            //控制发送者在发布到kafka之前等待批处理的字节数。 满足batch.size和ling.ms之一，producer便开始发送消息
            //默认16384   16kb
            props.put("batch.size", conf.getUnnecessaryValue(Key.BATCH_SIZE, "16384", null));
            props.put("linger.ms", 1);
            props.put("key.serializer", conf.getUnnecessaryValue(Key.KEYSERIALIZER, "org.apache.kafka.common.serialization.StringSerializer", null));
            props.put("value.serializer", conf.getUnnecessaryValue(Key.VALUESERIALIZER, "org.apache.kafka.common.serialization.StringSerializer", null));
            producer = new KafkaProducer<String, String>(props);
        }

        @Override
        public void prepare() {
            if (Boolean.valueOf(conf.getUnnecessaryValue(Key.NO_TOPIC_CREATE, "false", null))) {

                ListTopicsResult topicsResult = AdminClient.create(props).listTopics();
                String topic = conf.getNecessaryValue(Key.TOPIC, KafkaWriterErrorCode.REQUIRED_VALUE);

                try {
                    if (!topicsResult.names().get().contains(topic)) {
                        new NewTopic(
                                topic,
                                Integer.valueOf(conf.getUnnecessaryValue(Key.TOPIC_NUM_PARTITION, "1", null)),
                                Short.valueOf(conf.getUnnecessaryValue(Key.TOPIC_REPLICATION_FACTOR, "1", null))
                        );
                        List<NewTopic> newTopics = new ArrayList<NewTopic>();
                        AdminClient.create(props).createTopics(newTopics);
                    }
                } catch (Exception e) {
                    throw new DataXException(KafkaWriterErrorCode.CREATE_TOPIC, KafkaWriterErrorCode.REQUIRED_VALUE.getDescription());
                }
            }
        }

        @Override
        public void startWrite(RecordReceiver lineReceiver) {
            logger.info("start to writer kafka");
            Record record = null;
            while ((record = lineReceiver.getFromReader()) != null) {//说明还在读取数据,或者读取的数据没处理完
                //获取一行数据，按照指定分隔符 拼成字符串 发送出去
                if (conf.getUnnecessaryValue(Key.WRITE_TYPE, WriteType.TEXT.name(), null).toLowerCase()
                        .equals(WriteType.TEXT.name().toLowerCase())) {
                    producer.send(new ProducerRecord<String, String>(this.conf.getString(Key.TOPIC),
                            record.getColumn(0).asString(),
                            recordToString(record))
                    );
                } else if (conf.getUnnecessaryValue(Key.WRITE_TYPE, WriteType.TEXT.name(), null).toLowerCase()
                        .equals(WriteType.JSON.name().toLowerCase())) {
                    // 第一个字段为主键
                    producer.send(new ProducerRecord<String, String>(this.conf.getString(Key.TOPIC),
                            record.getColumn(0).asString(),
                            recordToString(record))
                    );
                }
//                logger.info("complete write " + record.toString());
                producer.flush();
            }

            logger.info("start Writer finished");
        }

        @Override
        public void destroy() {
            if (producer != null) {
                producer.close();
            }
        }

        /**
         * 数据格式化
         * 收到的数据是一个数组，需要构成按列的数据，组成json，所以需要组成map
         *
         * @param record
         * @return
         */
        private String recordToString(Record record) {
            String[] columns = this.columns;

            int recordLength = record.getColumnNumber();
            if (0 == recordLength) {
                return NEWLINE_FLAG;
            }

            recordLength = recordLength > columns.length ? columns.length : recordLength;
            Map<String, Object> jsonMap = new HashMap<String, Object>();

            Column column;

            for (int i = 0; i < recordLength; i++) {
                column = record.getColumn(i);

                jsonMap.put(columns[i], column.getRawData());
            }

            return JSON.toJSONString(jsonMap);
        }
    }
}
