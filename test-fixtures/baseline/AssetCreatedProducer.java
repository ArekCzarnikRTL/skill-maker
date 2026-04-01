package com.example.assets.producer;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.tracing.Tracer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
public class AssetCreatedProducer {

    private static final Logger log = LoggerFactory.getLogger(AssetCreatedProducer.class);

    private static final String TOPIC = "assets.material.events";
    private static final String CE_SPEC_VERSION = "1.0";
    private static final String CE_TYPE = "com.example.assets.asset.created";
    private static final String CE_SOURCE = "/assets/service";
    private static final String CE_DATA_CONTENT_TYPE = "application/json";

    private final KafkaTemplate<String, byte[]> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final Tracer tracer;

    public AssetCreatedProducer(KafkaTemplate<String, byte[]> kafkaTemplate,
                                ObjectMapper objectMapper,
                                Tracer tracer) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.tracer = tracer;
    }

    public void publish(AssetPayload asset) {
        try {
            String eventId = UUID.randomUUID().toString();
            String traceId = resolveTraceId();
            String timestamp = OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
            byte[] payload = objectMapper.writeValueAsBytes(asset);

            Message<byte[]> message = MessageBuilder
                    .withPayload(payload)
                    .setHeader(KafkaHeaders.TOPIC, TOPIC)
                    .setHeader(KafkaHeaders.KEY, asset.id())
                    // CloudEvents mandatory attributes as Kafka headers (binary content mode)
                    .setHeader("ce_specversion", CE_SPEC_VERSION)
                    .setHeader("ce_id", eventId)
                    .setHeader("ce_type", CE_TYPE)
                    .setHeader("ce_source", CE_SOURCE)
                    .setHeader("ce_time", timestamp)
                    .setHeader("ce_datacontenttype", CE_DATA_CONTENT_TYPE)
                    // Optional CloudEvents extension for tracing
                    .setHeader("ce_traceparent", traceId)
                    .build();

            kafkaTemplate.send(message)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.error("Failed to publish asset created event [eventId={}, assetId={}]",
                                    eventId, asset.id(), ex);
                        } else {
                            log.info("Published asset created event [eventId={}, assetId={}, topic={}, partition={}, offset={}]",
                                    eventId, asset.id(), TOPIC,
                                    result.getRecordMetadata().partition(),
                                    result.getRecordMetadata().offset());
                        }
                    });

        } catch (Exception e) {
            log.error("Error serializing asset created event for assetId={}", asset.id(), e);
            throw new RuntimeException("Failed to publish asset created event", e);
        }
    }

    private String resolveTraceId() {
        if (tracer != null && tracer.currentSpan() != null) {
            return tracer.currentSpan().context().traceId();
        }
        return UUID.randomUUID().toString();
    }

    /**
     * Immutable value object representing the asset payload carried in the event.
     */
    public record AssetPayload(
            String id,
            String name,
            String type,
            String ownerId,
            OffsetDateTime createdAt
    ) {
        public static AssetPayload of(String id, String name, String type, String ownerId) {
            return new AssetPayload(id, name, type, ownerId, OffsetDateTime.now());
        }
    }
}
