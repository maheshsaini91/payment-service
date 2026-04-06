package com.example.payment.kafka;

import com.example.payment.model.Payment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class KafkaPaymentEventPublisher implements PaymentEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(KafkaPaymentEventPublisher.class);

    private final KafkaTemplate<String, PaymentCompletedEvent> kafkaTemplate;
    private final String topic;

    public KafkaPaymentEventPublisher(
            KafkaTemplate<String, PaymentCompletedEvent> kafkaTemplate,
            @Value("${kafka.topic.payment-events}") String topic) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
    }

    @Override
    public void publishPaymentCompleted(Payment payment) {
        var event = new PaymentCompletedEvent(
                payment.getId(),
                payment.getPayerAccountId(),
                payment.getPayeeAccountId(),
                payment.getAmount(),
                payment.getCurrency(),
                Instant.now()
        );

        // Use paymentId as the Kafka partition key for ordering guarantees per payment
        kafkaTemplate.send(topic, payment.getId().toString(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish PaymentCompletedEvent for paymentId={}", payment.getId(), ex);
                    } else {
                        log.info("Published PaymentCompletedEvent paymentId={} offset={}",
                                payment.getId(), result.getRecordMetadata().offset());
                    }
                });
    }
}
