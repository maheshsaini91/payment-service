//package com.example.payment;
//
//import com.example.payment.kafka.KafkaPaymentEventPublisher;
//import com.example.payment.kafka.PaymentCompletedEvent;
//import com.example.payment.model.Payment;
//import org.apache.kafka.clients.consumer.ConsumerConfig;
//import org.apache.kafka.clients.consumer.ConsumerRecord;
//import org.apache.kafka.common.serialization.StringDeserializer;
//import org.junit.jupiter.api.AfterEach;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
//import org.springframework.kafka.core.KafkaTemplate;
//import org.springframework.kafka.support.serializer.JsonDeserializer;
//import org.springframework.kafka.test.EmbeddedKafkaBroker;
//import org.springframework.kafka.test.context.EmbeddedKafka;
//import org.springframework.kafka.test.utils.KafkaTestUtils;
//import org.springframework.test.annotation.DirtiesContext;
//import org.springframework.test.context.TestPropertySource;
//
//import java.math.BigDecimal;
//import java.util.Map;
//import java.util.UUID;
//
//import static org.assertj.core.api.Assertions.assertThat;
//
///**
// * Integration test: verifies that a PaymentCompletedEvent actually lands on the
// * {@code payment.events} Kafka topic with the correct payload.
// *
// * Uses Spring's embedded Kafka broker — no external infrastructure needed.
// */
//@SpringBootTest
//@EmbeddedKafka(
//        partitions = 1,
//        topics = "payment.events",
//        bootstrapServersProperty = "spring.kafka.bootstrap-servers"
//)
//@TestPropertySource(properties = {
//        "kafka.topic.payment-events=payment.events",
//        "ledger.base-url=http://localhost:9999" // never called in this test
//})
//@DirtiesContext
//class KafkaPaymentEventPublisherIT {
//
//    private static final String TOPIC = "payment.events";
//
//    @Autowired
//    private KafkaTemplate<String, PaymentCompletedEvent> kafkaTemplate;
//
//    @Autowired
//    private EmbeddedKafkaBroker embeddedKafka;
//
//    private KafkaPaymentEventPublisher publisher;
//    private org.apache.kafka.clients.consumer.Consumer<String, PaymentCompletedEvent> consumer;
//
//    @BeforeEach
//    void setUp() {
//        publisher = new KafkaPaymentEventPublisher(kafkaTemplate, TOPIC);
//
//        Map<String, Object> consumerProps = KafkaTestUtils.consumerProps("test-group", "true", embeddedKafka);
//        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
//        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
//        consumerProps.put(JsonDeserializer.TRUSTED_PACKAGES, "com.example.payment.kafka");
//        consumerProps.put(JsonDeserializer.VALUE_DEFAULT_TYPE, PaymentCompletedEvent.class.getName());
//
//        var factory = new DefaultKafkaConsumerFactory<String, PaymentCompletedEvent>(consumerProps);
//        consumer = factory.createConsumer();
//        embeddedKafka.consumeFromAnEmbeddedTopic(consumer, TOPIC);
//    }
//
//    @AfterEach
//    void tearDown() {
//        consumer.close();
//    }
//
//    @Test
//    void publishPaymentCompleted_sendsEventToTopic() throws Exception {
//        UUID payerId = UUID.randomUUID();
//        UUID payeeId = UUID.randomUUID();
//        var payment = new Payment(payerId, payeeId, new BigDecimal("150.00"), "AED", "rent", null);
//        payment.updateStatus(Payment.Status.COMPLETED);
//
//        publisher.publishPaymentCompleted(payment);
//
//        // Poll with a generous timeout to account for async send
//        ConsumerRecord<String, PaymentCompletedEvent> record =
//                KafkaTestUtils.getSingleRecord(consumer, TOPIC, 5_000);
//
//        assertThat(record).isNotNull();
//        // Partition key must be the payment id for ordering guarantees
//        assertThat(record.key()).isEqualTo(payment.getId().toString());
//
//        PaymentCompletedEvent event = record.value();
//        assertThat(event.paymentId()).isEqualTo(payment.getId());
//        assertThat(event.payerAccountId()).isEqualTo(payerId);
//        assertThat(event.payeeAccountId()).isEqualTo(payeeId);
//        assertThat(event.amount()).isEqualByComparingTo("150.00");
//        assertThat(event.currency()).isEqualTo("AED");
//        assertThat(event.completedAt()).isNotNull();
//    }
//}
