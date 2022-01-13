package com.alibaba.datax.plugin.reader.kafkareader;

import com.alibaba.datax.common.element.*;
import com.alibaba.datax.common.exception.DataXException;
import com.alibaba.datax.common.plugin.RecordSender;
import com.alibaba.datax.common.spi.Reader;
import com.alibaba.datax.common.util.Configuration;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.common.PartitionInfo;
import org.apache.kafka.common.TopicPartition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.Duration;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class KafkaReader extends Reader {

    public static class Job extends Reader.Job {
        private static final Logger logger = LoggerFactory
                .getLogger(Job.class);

        private Configuration originalConfig = null;


        @Override
        public void init() {
            this.originalConfig = super.getPluginJobConf();
            // warn: 忽略大小写

            String topic = this.originalConfig
                    .getString(Key.TOPIC);
            Integer partitions = this.originalConfig
                    .getInt(Key.TOPIC_NUM_PARTITION);
            String bootstrapServers = this.originalConfig
                    .getString(Key.BOOTSTRAP_SERVERS);


            String groupId = this.originalConfig
                    .getString(Key.GROUP_ID);
//            Integer columnCount = this.originalConfig
//                    .getInt(Key.COLUMNCOUNT);
//            String split = this.originalConfig.getString(Key.FIELD_DELIMITER);
//            String filterContaintsStr = this.originalConfig.getString(Key.CONTAINTS_STR);
//            String filterContaintsFlag = this.originalConfig.getString(Key.CONTAINTS_STR_FLAG);
//            String conditionAllOrOne = this.originalConfig.getString(Key.CONDITION_ALL_OR_ONE);
//            String parsingRules = this.originalConfig.getString(Key.PARSING_RULES);
            String writerOrder = this.originalConfig.getString(Key.WRITE_ORDER);
            String kafkaReaderColumns = this.originalConfig.getString(Key.COLUMNS);


            if (null == topic) {

                throw DataXException.asDataXException(KafkaReaderErrorCode.TOPIC_ERROR,
                        "没有设置参数[topic].");
            }
            if (partitions == null) {
                throw DataXException.asDataXException(KafkaReaderErrorCode.PARTITION_ERROR,
                        "没有设置参数[kafka.partitions].");
            } else if (partitions < 1) {
                throw DataXException.asDataXException(KafkaReaderErrorCode.PARTITION_ERROR,
                        "[kafka.partitions]不能小于1.");
            }
            if (null == bootstrapServers) {
                throw DataXException.asDataXException(KafkaReaderErrorCode.ADDRESS_ERROR,
                        "没有设置参数[bootstrap.servers].");
            }
            if (null == groupId) {
                throw DataXException.asDataXException(KafkaReaderErrorCode.KAFKA_READER_ERROR,
                        "没有设置参数[groupid].");
            }

            if (null == kafkaReaderColumns) {
                throw DataXException.asDataXException(KafkaReaderErrorCode.KAFKA_READER_ERROR,
                        "没有设置参数[columns].");
            }

        }

        @Override
        public void preCheck() {
            init();

        }

        @Override
        public List<Configuration> split(int adviceNumber) {
            List<Configuration> configurations = new ArrayList<Configuration>();

            Integer partitions = this.originalConfig.getInt(Key.TOPIC_NUM_PARTITION);
            for (int i = 0; i < partitions; i++) {
                configurations.add(this.originalConfig.clone());
            }
            return configurations;
        }

        @Override
        public void post() {
        }

        @Override
        public void destroy() {

        }

    }

    public static class Task extends Reader.Task {

        private static final Logger LOG = LoggerFactory
                .getLogger(KafkaReader.Task.class);
        //配置文件
        private Configuration readerSliceConfig;
        //kafka消息的分隔符
        private String split;
        //解析规则
        private String parsingRules;
        //是否停止拉去数据
        private boolean flag;
        //kafka address
        private String bootstrapServers;
        //kafka groupid
        private String groupId;
        //kafkatopic
        private String kafkaTopic;
        //kafka中的数据一共有多少个字段
        private int count;
        //是否需要data_from
        //kafka ip 端口+ topic
        //将包含/不包含该字符串的数据过滤掉
        private String filterContaintsStr;
        //是包含containtsStr 还是不包含
        //1 表示包含 0 表示不包含
        private int filterContaintsStrFlag;
        //全部包含或不包含&#xff0c;包含其中一个或者不包含其中一个。
        private int conditionAllOrOne;
        //writer端要求的顺序。
        private String writerOrder;
        //kafkareader记录的 作为key的属性，如果没有这个key，输出的时候
        private String kafkaReaderColumns;
        private Date fromTime;
        private Date endTime;
        //异常文件路径
        private String exceptionPath;

        @Override
        public void init() {
            flag = true;
            this.readerSliceConfig = super.getPluginJobConf();
            split = this.readerSliceConfig.getString(Key.FIELD_DELIMITER);
            bootstrapServers = this.readerSliceConfig.getString(Key.BOOTSTRAP_SERVERS);
            groupId = this.readerSliceConfig.getString(Key.GROUP_ID);
            kafkaTopic = this.readerSliceConfig.getString(Key.TOPIC);
//            count = this.readerSliceConfig.getInt(Key.COLUMNCOUNT);
//            filterContaintsStr = this.readerSliceConfig.getString(Key.CONTAINTS_STR);
//            filterContaintsStrFlag = this.readerSliceConfig.getInt(Key.CONTAINTS_STR_FLAG);
//            conditionAllOrOne = this.readerSliceConfig.getInt(Key.CONTAINTS_STR_FLAG);
//            parsingRules = this.readerSliceConfig.getString(Key.PARSING_RULES);
//            writerOrder = this.readerSliceConfig.getString(Key.WRITER_ORDER);
            kafkaReaderColumns = this.readerSliceConfig.getString(Key.COLUMNS);
//            exceptionPath = this.readerSliceConfig.getString(Key.EXECPTION_PATH);


            LOG.info(filterContaintsStr);
        }

        @Override
        public void startRead(RecordSender recordSender) {

            Properties props = new Properties();
            props.put("bootstrap.servers", bootstrapServers);
            props.put("group.id", groupId != null ? groupId : UUID.randomUUID().toString());
            props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
            props.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
            props.put("enable.auto.commit", "false");
            KafkaConsumer<String, String> consumer = new KafkaConsumer<String, String>(props);
            consumer.subscribe(Collections.singletonList(kafkaTopic));
            Record oneRecord = null;
            while (flag) {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(30));
                for (ConsumerRecord<String, String> record : records) {

                    String value = record.value();
                    //定义过滤标志

                    oneRecord = buildOneRecord(recordSender, value);
                    //如果返回值不等于null表示不是异常消息。
                    if (oneRecord != null) {
                        recordSender.sendToWriter(oneRecord);
                    }
                }
                consumer.commitSync();
                //判断当前事件是不是0点,0点的话进程他退出 让job可以每天执行一次
                Date date = new Date();
                if (DateUtil.targetFormat(date).split(" ")[1].substring(0, 2).equals("00")) {
                    destroy();
                }

            }
        }

        private int filterMessage(String value) {
            //如果要过滤的条件配置了
            int ifNotContinue = 0;

            if (filterContaintsStr != null) {
                String[] filterStrs = filterContaintsStr.split(",");
                //所有
                if (conditionAllOrOne == 1) {
                    //过滤掉包含filterContaintsStr的所有项的值。
                    if (filterContaintsStrFlag == 1) {
                        int i = 0;
                        for (; i < filterStrs.length; i++) {
                            if (!value.contains(filterStrs[i])) break;
                        }
                        if (i >= filterStrs.length) ifNotContinue = 1;
                    } else {
                        //留下掉包含filterContaintsStr的所有项的值
                        int i = 0;
                        for (; i < filterStrs.length; i++) {
                            if (!value.contains(filterStrs[i])) break;
                        }
                        if (i < filterStrs.length) ifNotContinue = 1;
                    }

                } else {
                    //过滤掉包含其中一项的值
                    if (filterContaintsStrFlag == 1) {
                        int i = 0;
                        for (; i < filterStrs.length; i++) {
                            if (value.contains(filterStrs[i])) break;
                        }
                        if (i < filterStrs.length) ifNotContinue = 1;
                    }
                    //留下包含其中一下的值
                    else {
                        int i = 0;
                        for (; i < filterStrs.length; i++) {
                            if (value.contains(filterStrs[i])) break;
                        }
                        if (i >= filterStrs.length) ifNotContinue = 1;
                    }
                }
            }
            return ifNotContinue;

        }

        /**
         * 收到的kafka消息，一般为json数据，但datax中间数据格式时数组，所以需要把，需要用到的数据转换成数组，这样，收到的writer，可以知道数据格式是怎么样的
         *
         * @param recordSender
         * @param value
         * @return
         */
        private Record buildOneRecord(RecordSender recordSender, String value) {
            Record record = parseJson(value, recordSender);
//            record.addColumn(new StringColumn(value));
//            if (parsingRules.equals("regex")) {
//                record = parseRegex(value, recordSender);
//            } else if (parsingRules.equals("json")) {
//                record = parseJson(value, recordSender);
////            } else if (parsingRules.equals("split")) {
////                record = parseSplit(value, recordSender);
////            }
            return record;
        }

        private Record parseSplit(String value, RecordSender recordSender) {
            Record record = recordSender.createRecord();
            String[] splits = value.split(this.split);
            if (splits.length != count) {
                writerErrorPath(value);
                return null;
            }
            parseOrders(Arrays.asList(splits), record);
            return record;
        }

        /**
         * 一般kafka传输的都是json，所以这里需要把json数据解析成map，然后像 select 一样读取出列，按照列名字显示
         * 要查询哪些列 由 kafkaReaderColumns定义，这些列会加入列表中
         * @param value
         * @param recordSender
         * @return
         */
        private Record parseJson(String value, RecordSender recordSender) {
            Job.logger.info("kafka msg: {}", value);
            Record record = recordSender.createRecord();
            Map<String, Object> map = JSON.parseObject(value);

            String[] columns = kafkaReaderColumns.split(",");
            List<Object> datas = new ArrayList<>();
            for (String column : columns) {
                datas.add(map.get(column));
            }

//            if (datas.size() != columns.length) {
//                writerErrorPath(value);
//                return null;
//            }

            parseOrders(datas, record);
            Job.logger.info("transform: {}", record.toString());

            return record;
        }

//        private Record parseRegex(String value, RecordSender recordSender) {
//            Record record = recordSender.createRecord();
//            ArrayList<String> datas = new ArrayList<String>();
//            Pattern r = Pattern.compile(split);
//            Matcher m = r.matcher(value);
//            if (m.find()) {
//                if (m.groupCount() != count) {
//                    writerErrorPath(value);
//                }
//                for (int i = 1; i <= count; i++) {
//                    //  record.addColumn(new StringColumn(m.group(i)));
//                    datas.add(m.group(i));
//                    return record;
//                }
//            } else {
//                writerErrorPath(value);
//            }
//
//            parseOrders(datas, record);
//
//            return null;
//        }

        private void writerErrorPath(String value) {
            if (exceptionPath == null) return;
            FileOutputStream fileOutputStream = null;
            try {
                fileOutputStream = getFileOutputStream();
                fileOutputStream.write((value + "\n").getBytes());
                fileOutputStream.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private FileOutputStream getFileOutputStream() throws FileNotFoundException {
            return new FileOutputStream(exceptionPath + "/" + kafkaTopic + "errordata" + DateUtil.targetFormat(new Date(), "yyyyMMdd"), true);
        }

        private void parseOrders(List<Object> datas, Record record) {
            //writerOrder
            String[] orders = null;
            if (writerOrder != null) {
                orders = writerOrder.split(",");
                for (String order : orders) {
                    if (order.equals("data_from")) {
                        record.addColumn(new StringColumn(bootstrapServers + "|" + kafkaTopic));
                    } else if (order.equals("uuid")) {
                        record.addColumn(new StringColumn(UUID.randomUUID().toString()));
                    } else if (order.equals("null")) {
                        record.addColumn(new StringColumn("null"));
                    } else if (order.equals("datax_time")) {
                        record.addColumn(new StringColumn(DateUtil.targetFormat(new Date())));
                    } else if (isNumeric(order)) {
                        Object o = datas.get(new Integer(order) - 1);
                        add2Record(record, o);
                    }
                }
            }else {
                for (Object o : datas) {
                    add2Record(record, o);
                }
            }

        }

        /**
         * 要根据对象的类型，把数据添加到记录中
         *
         * @param record
         * @param o
         */
        private void add2Record(Record record, Object o) {
            Column column = null;
            if (o instanceof Long) {
                column = new LongColumn(((Long) o).longValue());
            }else if (o instanceof Double) {
                column = new DoubleColumn(((Double) o).floatValue());
            }else {
                column = new StringColumn(o.toString());
            }

            record.addColumn(column);
        }

        public static boolean isNumeric(String str) {
            for (int i = 0; i < str.length(); i++) {
                if (!Character.isDigit(str.charAt(i))) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public void post() {
        }

        @Override
        public void destroy() {
            flag = false;
        }


    }
}