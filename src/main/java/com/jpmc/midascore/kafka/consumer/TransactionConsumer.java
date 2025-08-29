package com.jpmc.midascore.kafka.consumer;

import com.jpmc.midascore.foundation.Transaction;
import com.jpmc.midascore.service.TransactionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class TransactionConsumer {
    static final Logger logger = LoggerFactory.getLogger(TransactionConsumer.class);

    private final TransactionService transactionService;

    public TransactionConsumer(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @KafkaListener(topics = "${general.kafka-topic}", groupId = "${spring.kafka.consumer.group-id}")
    public void consume(Transaction transaction) {
        logger.info("Consumed transaction: {}", transaction);
        transactionService.processTransaction(transaction)
                .ifPresent(record -> logger.info("Transaction recorded: {}", record));
    }
}
