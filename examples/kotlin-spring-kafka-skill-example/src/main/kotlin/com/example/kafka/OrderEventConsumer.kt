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

    @KafkaListener(topics = ["rlt.orders.events"], groupId = "order-event-consumer")
    fun consume(record: ConsumerRecord<String, OrderEvent>) {
        val headers = extractEnvelopeHeaders(record)

        log.info(
            "Received OrderEvent: id={}, source={}, type={}, time={}, traceparent={}, key={}, orderId={}",
            headers["ce_id"],
            headers["ce_source"],
            headers["ce_type"],
            headers["ce_time"],
            headers["ce_traceparent"],
            record.key(),
            record.value().orderId
        )

        val ceId = headers["ce_id"]
        if (ceId != null && isDuplicate(ceId)) {
            log.warn("Duplicate event detected, skipping: ce_id={}", ceId)
            return
        }

        processOrder(record.value(), headers)
    }

    private fun processOrder(order: OrderEvent, headers: Map<String, String>) {
        log.info(
            "Processing order: orderId={}, customerId={}, amount={} {}, correlationId={}",
            order.orderId,
            order.customerId,
            order.amount,
            order.currency,
            headers["ce_correlationid"]
        )
        // TODO: implement business logic
    }

    private fun extractEnvelopeHeaders(record: ConsumerRecord<String, OrderEvent>): Map<String, String> {
        val headers = mutableMapOf<String, String>()
        for (header in record.headers()) {
            if (header.key().startsWith("ce_") || header.key() == "content-type") {
                headers[header.key()] = String(header.value(), StandardCharsets.UTF_8)
            }
        }
        return headers
    }

    private fun isDuplicate(ceId: String): Boolean {
        // TODO: implement idempotency check (e.g. against a persistent store)
        return false
    }
}
