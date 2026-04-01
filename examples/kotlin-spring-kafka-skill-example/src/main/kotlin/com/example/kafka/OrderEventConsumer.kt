package com.example.kafka

import com.example.kafka.avro.OrderEvent
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component
import java.nio.charset.StandardCharsets

@Component
class OrderEventConsumer {

    private val log = LoggerFactory.getLogger(OrderEventConsumer::class.java)

    @KafkaListener(topics = ["rlt.oders.events"], groupId = "\${spring.kafka.consumer.group-id}")
    fun consume(record: ConsumerRecord<String, OrderEvent>) {
        val envelope = extractEnvelope(record)

        log.info(
            "Received event id={} type={} source={} traceparent={} key={}",
            envelope.id, envelope.type, envelope.source, envelope.traceparent, record.key()
        )
        // Restore tracing context from ce_traceparent
        // (integrate with your tracing library, e.g. Micrometer / OpenTelemetry)
        // Idempotency check using ce_id
        if (isDuplicate(envelope.id)) {
            log.warn("Duplicate event id={}, skipping", envelope.id)
            return
        }

        val orderEvent = record.value()
        handleOrderEvent(orderEvent, envelope)
    }

    private fun handleOrderEvent(event: OrderEvent, envelope: CloudEventEnvelope) {
        log.info(
            "Processing order: orderId={} customerId={} amount={} {} correlationId={}",
            event.orderId, event.customerId, event.amount, event.currency, envelope.correlationId
        )
        // TODO: implement business logic
    }

    private fun isDuplicate(eventId: String): Boolean {
        // TODO: implement idempotency check (e.g. store processed ce_id values)
        return false
    }

    private fun extractEnvelope(record: ConsumerRecord<String, OrderEvent>): CloudEventEnvelope {
        fun header(name: String): String? =
            record.headers().lastHeader(name)?.value()?.toString(StandardCharsets.UTF_8)

        return CloudEventEnvelope(
            id = header("ce_id") ?: error("Missing required header ce_id"),
            source = header("ce_source") ?: error("Missing required header ce_source"),
            specversion = header("ce_specversion") ?: error("Missing required header ce_specversion"),
            type = header("ce_type") ?: error("Missing required header ce_type"),
            time = header("ce_time") ?: error("Missing required header ce_time"),
            traceparent = header("ce_traceparent") ?: error("Missing required header ce_traceparent"),
            correlationId = header("ce_correlationid"),
            causationId = header("ce_causationid"),
            subject = header("ce_subject"),
            dataContentType = header("content-type"),
        )
    }
}

data class CloudEventEnvelope(
    val id: String,
    val source: String,
    val specversion: String,
    val type: String,
    val time: String,
    val traceparent: String,
    val correlationId: String?,
    val causationId: String?,
    val subject: String?,
    val dataContentType: String?,
)
