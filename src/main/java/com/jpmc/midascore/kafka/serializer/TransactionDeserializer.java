package com.jpmc.midascore.kafka.serializer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jpmc.midascore.foundation.Transaction;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.Deserializer;

public class TransactionDeserializer implements Deserializer<Transaction> {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Transaction deserialize(String topic, byte[] data) {
        if (data == null) {
            return null;
        }
        try {
            return objectMapper.readValue(data, Transaction.class);
        } catch (Exception e) {
            throw new SerializationException("Error deserializing Transaction", e);
        }
    }
}