package com.example.assets;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
public class AssetProducer {

    private final KafkaTemplate<String, byte[]> kafkaTemplate;

    public AssetProducer(KafkaTemplate<String, byte[]> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishAssetCreated(String assetId, byte[] payload) {
        RecordHeaders headers = new RecordHeaders();
        // Anti-pattern: no ce_ prefix
        headers.add("type", "asset-updated".getBytes());
        // Anti-pattern: source as environment
        headers.add("source", "production".getBytes());
        // Anti-pattern: missing id, specversion, time, traceparent

        ProducerRecord<String, byte[]> record = new ProducerRecord<>(
            "assets.material.events",
            null,
            assetId,
            payload,
            headers
        );

        kafkaTemplate.send(record);
    }
}
