package com.netrtl.assets.kafka;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.Headers;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * Publishes an AssetCreated domain event to the assets.material.events topic.
 *
 * Envelope headers follow the CloudEvents Kafka Protocol Binding (binary content mode).
 * All CloudEvents attributes are transported as Kafka message headers with the "ce_" prefix.
 * Business payload (Avro) is carried in the message value only.
 *
 * Spec reference: envelope-spec.md
 */
@Component
public class AssetCreatedProducer {

    private static final String TOPIC           = "assets.material.events";
    private static final String CE_SOURCE       = "https://netrtl.com/assets/asset-management";
    private static final String CE_SPECVERSION  = "1.0";
    private static final String CE_TYPE         = "assets.material.asset-created.v1";
    private static final String CONTENT_TYPE    = "application/avro";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public AssetCreatedProducer(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    /**
     * Publishes an asset-created event.
     *
     * @param assetId        UUID of the created asset (used as message key and subject component)
     * @param assetType      Business type of the asset (e.g. "small-material")
     * @param payload        Avro-serialized event payload
     * @param correlationId  Business correlation ID to propagate across boundaries (SHOULD)
     * @param causationId    ID of the causing event, if known (SHOULD)
     * @param traceparent    W3C Trace Context from the incoming request/span (MUST be propagated)
     */
    public void publish(
            String assetId,
            String assetType,
            Object payload,
            String correlationId,
            String causationId,
            String traceparent) {

        // --- Derived values ---
        String eventId  = UUID.randomUUID().toString();
        String subject  = assetType + "/" + assetId;   // e.g. "small-material/287fc583-..."
        String time     = DateTimeFormatter.ISO_INSTANT.format(Instant.now());

        // Kafka message key is derived from subject (entity UUID) to guarantee ordering per entity
        String messageKey = assetId;

        // If no tracing context is available, create a new traceparent
        String resolvedTraceparent = (traceparent != null && !traceparent.isBlank())
                ? traceparent
                : generateTraceparent();

        ProducerRecord<String, Object> record = new ProducerRecord<>(TOPIC, messageKey, payload);
        Headers headers = record.headers();

        // ---- MUST headers (Required) ----
        setHeader(headers, "ce_id",          eventId);
        setHeader(headers, "ce_source",      CE_SOURCE);
        setHeader(headers, "ce_specversion", CE_SPECVERSION);
        setHeader(headers, "ce_type",        CE_TYPE);
        setHeader(headers, "ce_time",        time);
        setHeader(headers, "ce_traceparent", resolvedTraceparent);

        // ---- SHOULD headers (Recommended) ----
        if (correlationId != null && !correlationId.isBlank()) {
            setHeader(headers, "ce_correlationid", correlationId);
        }
        if (causationId != null && !causationId.isBlank()) {
            setHeader(headers, "ce_causationid", causationId);
        }

        // ---- MAY headers (Optional) ----
        setHeader(headers, "ce_subject",  subject);
        setHeader(headers, "content-type", CONTENT_TYPE);  // NOTE: no "ce_" prefix per spec

        kafkaTemplate.send(record);
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    /** Writes a UTF-8 encoded string value into a Kafka header. */
    private static void setHeader(Headers headers, String key, String value) {
        headers.add(key, value.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Generates a minimal valid W3C traceparent when no existing trace context is available.
     * Format: 00-<traceId-32hex>-<parentId-16hex>-01
     */
    private static String generateTraceparent() {
        String traceId  = UUID.randomUUID().toString().replace("-", "")
                        + UUID.randomUUID().toString().replace("-", "");
        traceId = traceId.substring(0, 32);
        String parentId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        return "00-" + traceId + "-" + parentId + "-01";
    }
}
