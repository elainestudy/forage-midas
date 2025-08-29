package com.jpmc.midascore.service;

import com.jpmc.midascore.dto.Incentive;
import com.jpmc.midascore.entity.TransactionRecord;
import com.jpmc.midascore.entity.UserRecord;
import com.jpmc.midascore.foundation.Transaction;
import com.jpmc.midascore.repository.TransactionRecordRepository;
import com.jpmc.midascore.repository.UserRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for TransactionService using Mockito's dependency injection.
 * Enables Mockito annotations with @ExtendWith(MockitoExtension.class).
 */
@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    // Creates mock objects with the @Mock annotation.
    @Mock
    private UserRepository userRepository;
    @Mock
    private TransactionRecordRepository transactionRecordRepository;
    @Mock
    private RestTemplate restTemplate;

    // Automatically creates a service instance and injects the mock objects.
    @InjectMocks
    private TransactionService transactionService;

    // Defines the URL as a static constant.
    private static final String INCENTIVE_API_URL = "http://localhost:8080/incentive";

    @BeforeEach
    void setupUrl() {
        ReflectionTestUtils.setField(
                transactionService,
                "incentiveApiUrl",
                INCENTIVE_API_URL
        );
    }
    /**
     * Tests that a transaction is successfully processed when both sender and recipient exist
     * and sender has sufficient funds, without any incentive applied.
     *
     * <p>Verifies that balances are updated correctly and a TransactionRecord is saved.
     */
    @Test
    void testProcessTransactionSuccess() {
        UserRecord sender = new UserRecord("Alice", 100f);
        UserRecord recipient = new UserRecord("Bob", 50f);
        Transaction transaction = new Transaction(1L, 2L, 30f);

        // Sets default incentive amount to 0 for this test case.
        when(restTemplate.postForObject(
                eq(INCENTIVE_API_URL),
                any(Object.class),
                eq(Incentive.class)
        )).thenReturn(new Incentive(0f));

        // Mocks repository responses.
        Mockito.when(userRepository.findById(1L)).thenReturn(Optional.of(sender));
        Mockito.when(userRepository.findById(2L)).thenReturn(Optional.of(recipient));

        Optional<TransactionRecord> result = transactionService.processTransaction(transaction);

        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(70f, sender.getBalance());
        Assertions.assertEquals(80f, recipient.getBalance());
        verify(transactionRecordRepository).save(any());
    }

    /**
     * Tests that a transaction is discarded when the sender has insufficient funds.
     *
     * <p>Verifies that balances remain unchanged and no TransactionRecord is saved.
     */
    @Test
    void testProcessTransactionInsufficientFunds() throws Exception {
        UserRecord sender = new UserRecord("Alice", 20f);  // less than transaction amount
        UserRecord recipient = new UserRecord("Bob", 50f);

        Transaction transaction = new Transaction(1L, 2L, 30f);  // amount > sender balance

        Mockito.when(userRepository.findById(1L)).thenReturn(Optional.of(sender));
        Mockito.when(userRepository.findById(2L)).thenReturn(Optional.of(recipient));

        Optional<TransactionRecord> result = transactionService.processTransaction(transaction);

        Assertions.assertTrue(result.isEmpty());
        Assertions.assertEquals(20f, sender.getBalance());
        Assertions.assertEquals(50f, recipient.getBalance());
        verify(transactionRecordRepository, Mockito.never()).save(any());
    }

    /**
     * Tests that a transaction is discarded when the recipient does not exist.
     *
     * <p>Verifies that the sender's balance remains unchanged and no TransactionRecord is saved.
     */
    @Test
    void testProcessTransactionRecipientNotFound() {
        UserRecord sender = new UserRecord("Alice", 100f);
        Transaction transaction = new Transaction(1L, 2L, 30f);

        // Uses eq() to ensure matching for Long parameters.
        when(userRepository.findById(eq(1L))).thenReturn(Optional.of(sender));
        when(userRepository.findById(eq(2L))).thenReturn(Optional.empty());

        Optional<TransactionRecord> result = transactionService.processTransaction(transaction);

        Assertions.assertTrue(result.isEmpty());
        Assertions.assertEquals(100f, sender.getBalance());
        verify(transactionRecordRepository, Mockito.never()).save(any());
    }

    /**
     * Tests that a transaction is processed successfully when a positive incentive is returned.
     *
     * <p>Verifies that recipient's balance is increased by both the transaction amount and the incentive,
     * sender's balance is decreased accordingly, and a TransactionRecord is saved.
     */
    @Test
    void testProcessTransactionWithPositiveIncentive() {
        UserRecord sender = new UserRecord("Alice", 100f);
        UserRecord recipient = new UserRecord("Bob", 50f);
        Transaction transaction = new Transaction(1L, 2L, 30f);

        // Stubs the incentive API call to return a positive incentive.
        when(restTemplate.postForObject(
                eq(INCENTIVE_API_URL), // Matches the specific URL string.
                any(Object.class),     // Matches any Object type.
                eq(Incentive.class)    // Matches the Incentive class.
        )).thenReturn(new Incentive(5f));

        when(userRepository.findById(1L)).thenReturn(Optional.of(sender));
        when(userRepository.findById(2L)).thenReturn(Optional.of(recipient));

        Optional<TransactionRecord> result = transactionService.processTransaction(transaction);

        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(70f, sender.getBalance());
        Assertions.assertEquals(85f, recipient.getBalance());
        verify(transactionRecordRepository).save(any());
    }

    /**
     * Tests that a transaction is processed successfully when the incentive API returns null.
     *
     * <p>Verifies that recipient's balance is only increased by the transaction amount,
     * sender's balance is decreased accordingly, and a TransactionRecord is saved.
     */
    @Test
    void testProcessTransactionWithNullIncentive() {
        UserRecord sender = new UserRecord("Alice", 100f);
        UserRecord recipient = new UserRecord("Bob", 50f);
        Transaction transaction = new Transaction(1L, 2L, 30f);

        // Stubs the incentive API call to return null.
        when(restTemplate.postForObject(
                eq(INCENTIVE_API_URL),
                any(Object.class),
                eq(Incentive.class)
        )).thenReturn(null);

        // Uses eq() to ensure matching for Long parameters.
        when(userRepository.findById(eq(1L))).thenReturn(Optional.of(sender));
        when(userRepository.findById(eq(2L))).thenReturn(Optional.of(recipient));

        Optional<TransactionRecord> result = transactionService.processTransaction(transaction);

        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(70f, sender.getBalance());
        Assertions.assertEquals(80f, recipient.getBalance());
        verify(transactionRecordRepository).save(any());
    }

    /**
     * Tests that a transaction is processed successfully when a negative incentive is returned.
     *
     * <p>Verifies that negative incentives do not reduce the recipient's balance,
     * sender's balance is decreased by the transaction amount, and a TransactionRecord is saved.
     */
    @Test
    void testProcessTransactionWithNegativeIncentive() {
        UserRecord sender = new UserRecord("Alice", 100f);
        UserRecord recipient = new UserRecord("Bob", 50f);
        Transaction transaction = new Transaction(1L, 2L, 30f);

        // Stubs the incentive API call to return a negative incentive.
        when(restTemplate.postForObject(
                eq(INCENTIVE_API_URL),
                any(Object.class),
                eq(Incentive.class)
        )).thenReturn(new Incentive(-10f));

        // Uses eq() to ensure matching for Long parameters.
        when(userRepository.findById(eq(1L))).thenReturn(Optional.of(sender));
        when(userRepository.findById(eq(2L))).thenReturn(Optional.of(recipient));

        Optional<TransactionRecord> result = transactionService.processTransaction(transaction);

        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(70f, sender.getBalance());
        Assertions.assertEquals(80f, recipient.getBalance()); // negative incentive is ignored
        verify(transactionRecordRepository).save(any());
    }

    /**
     * Tests that a transaction is processed successfully when the incentive API throws an exception.
     *
     * <p>Verifies that the transaction is completed with a 0 incentive, as if the API returned 0.
     */
    @Test
    void testProcessTransactionWithIncentiveApiFailure() {
        UserRecord sender = new UserRecord("Alice", 100f);
        UserRecord recipient = new UserRecord("Bob", 50f);
        Transaction transaction = new Transaction(1L, 2L, 30f);

        // Stubs restTemplate behavior to throw an exception when called.
        when(restTemplate.postForObject(
                eq(INCENTIVE_API_URL),
                any(Object.class),
                eq(Incentive.class)
        )).thenThrow(new RuntimeException("Incentive API is down."));

        // Uses eq() to ensure matching for Long parameters.
        when(userRepository.findById(1L)).thenReturn(Optional.of(sender));
        when(userRepository.findById(2L)).thenReturn(Optional.of(recipient));

        Optional<TransactionRecord> result = transactionService.processTransaction(transaction);

        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(70f, sender.getBalance());
        Assertions.assertEquals(80f, recipient.getBalance()); // incentive is 0 due to exception
        verify(transactionRecordRepository).save(any());
    }
}
