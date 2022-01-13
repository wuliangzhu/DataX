package com.alibaba.datax.plugin.reader.kafkareader;

import com.alibaba.datax.common.spi.ErrorCode;

public enum  KafkaReaderErrorCode implements ErrorCode {
    PARTITION_ERROR("KafkaReader-00", "您缺失了必须填写的参数值.partition"),
    KAFKA_READER_ERROR("KafkaReader-01", ""),
    ADDRESS_ERROR("KafkaReader-02", "kafka地址错误"),
    TOPIC_ERROR("KafkaReader-03", "读数据前检查topic或是创建topic失败."),
    PARAMETER_ERROR("KafkaReader-04", "您缺失了必须填写的参数值")
    ;

    private final String code;
    private final String description;

    private KafkaReaderErrorCode(String code, String description) {
        this.code = code;
        this.description = description;
    }

    @Override
    public String getCode() {
        return this.code;
    }

    @Override
    public String getDescription() {
        return this.description;
    }

    @Override
    public String toString() {
        return String.format("Code:[%s], Description:[%s].", this.code,
                this.description);
    }
}
