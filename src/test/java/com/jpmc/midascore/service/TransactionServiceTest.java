package com.jpmc.midascore.service;

import com.jpmc.midascore.entity.TransactionRecord;
import com.jpmc.midascore.entity.UserRecord;
import com.jpmc.midascore.foundation.Transaction;
import com.jpmc.midascore.repository.TransactionRecordRepository;
import com.jpmc.midascore.repository.UserRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.Optional;

@SpringBootTest
class TransactionServiceTest {

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private TransactionRecordRepository transactionRecordRepository;

    @Autowired
    private TransactionService transactionService;

    /**
     * Tests that a transaction is processed successfully when:
     * <ul>
     *     <li>Sender exists and has sufficient balance</li>
     *     <li>Recipient exists</li>
     *     <li>Transaction amount is positive</li>
     * </ul>
     *
     * <p>Verifies that balances are updated correctly and a TransactionRecord is saved.
     */
    @Test
    void testProcessTransactionSuccess() throws Exception {
        // Create UserRecord objects using existing constructor
        UserRecord sender = new UserRecord("Alice", 100f);
        UserRecord recipient = new UserRecord("Bob", 50f);

        // Set id via reflection to simulate database return
        java.lang.reflect.Field idField = UserRecord.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(sender, 1L);
        idField.set(recipient, 2L);

        Transaction transaction = new Transaction(1L, 2L, 30f);

        // Mock repository responses
        Mockito.when(userRepository.findById(1L)).thenReturn(Optional.of(sender));
        Mockito.when(userRepository.findById(2L)).thenReturn(Optional.of(recipient));

        Optional<TransactionRecord> result = transactionService.processTransaction(transaction);

        // Verify transaction processed successfully
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(70f, sender.getBalance());
        Assertions.assertEquals(80f, recipient.getBalance());

        // Verify transactionRecordRepository.save was called
        Mockito.verify(transactionRecordRepository).save(Mockito.any());
    }

    /**
     * Tests that a transaction is discarded when sender has insufficient funds.
     *
     * <p>Verifies that no balances are changed and no TransactionRecord is saved.
     */
    @Test
    void testProcessTransactionInsufficientFunds() throws Exception {
        UserRecord sender = new UserRecord("Alice", 20f);  // less than transaction amount
        UserRecord recipient = new UserRecord("Bob", 50f);

        // Set id via reflection
        java.lang.reflect.Field idField = UserRecord.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(sender, 1L);
        idField.set(recipient, 2L);

        Transaction transaction = new Transaction(1L, 2L, 30f);  // amount > sender balance

        Mockito.when(userRepository.findById(1L)).thenReturn(Optional.of(sender));
        Mockito.when(userRepository.findById(2L)).thenReturn(Optional.of(recipient));

        Optional<TransactionRecord> result = transactionService.processTransaction(transaction);

        Assertions.assertTrue(result.isEmpty());
        Assertions.assertEquals(20f, sender.getBalance());  // unchanged
        Assertions.assertEquals(50f, recipient.getBalance());  // unchanged
        Mockito.verify(transactionRecordRepository, Mockito.never()).save(Mockito.any());
    }

    /**
     * Tests that a transaction is discarded when sender does not exist.
     *
     * <p>Verifies that no balances are changed and no TransactionRecord is saved.
     */
    @Test
    void testProcessTransactionSenderNotFound() {
        UserRecord recipient = new UserRecord("Bob", 50f);

        // Set id via reflection
        java.lang.reflect.Field idField;
        try {
            idField = UserRecord.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(recipient, 2L);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        Transaction transaction = new Transaction(1L, 2L, 30f);

        Mockito.when(userRepository.findById(1L)).thenReturn(Optional.empty());  // sender missing
        Mockito.when(userRepository.findById(2L)).thenReturn(Optional.of(recipient));

        Optional<TransactionRecord> result = transactionService.processTransaction(transaction);

        Assertions.assertTrue(result.isEmpty());
        Assertions.assertEquals(50f, recipient.getBalance()); // unchanged
        Mockito.verify(transactionRecordRepository, Mockito.never()).save(Mockito.any());
    }

    /**
     * Tests that a transaction is discarded when recipient does not exist.
     *
     * <p>Verifies that no balances are changed and no TransactionRecord is saved.
     */
    @Test
    void testProcessTransactionRecipientNotFound() {
        UserRecord sender = new UserRecord("Alice", 100f);

        // Set id via reflection
        java.lang.reflect.Field idField;
        try {
            idField = UserRecord.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(sender, 1L);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        Transaction transaction = new Transaction(1L, 2L, 30f);

        Mockito.when(userRepository.findById(1L)).thenReturn(Optional.of(sender));
        Mockito.when(userRepository.findById(2L)).thenReturn(Optional.empty()); // recipient missing

        Optional<TransactionRecord> result = transactionService.processTransaction(transaction);

        Assertions.assertTrue(result.isEmpty());
        Assertions.assertEquals(100f, sender.getBalance()); // unchanged
        Mockito.verify(transactionRecordRepository, Mockito.never()).save(Mockito.any());
    }
}
