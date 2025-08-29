package com.jpmc.midascore.service;

import com.jpmc.midascore.dto.Incentive;
import com.jpmc.midascore.entity.TransactionRecord;
import com.jpmc.midascore.entity.UserRecord;
import com.jpmc.midascore.exception.InvalidTransactionException;
import com.jpmc.midascore.foundation.Transaction;
import com.jpmc.midascore.repository.TransactionRecordRepository;
import com.jpmc.midascore.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;

@Service
public class TransactionService {
    static final Logger logger = LoggerFactory.getLogger(TransactionService.class);

    private final UserRepository userRepository;
    private final TransactionRecordRepository transactionRecordRepository;
    private final RestTemplate restTemplate;

    @Value("${incentive.api.url:http://localhost:8080/incentive}")
    private String incentiveApiUrl;

    public TransactionService(UserRepository userRepository,
                              TransactionRecordRepository transactionRecordRepository, RestTemplate restTemplate) {
        this.userRepository = userRepository;
        this.transactionRecordRepository = transactionRecordRepository;
        this.restTemplate = restTemplate;
    }

    /**
     * Processes a transaction by validating it and recording it to the database.
     *
     * <p>Business logic:
     * <ul>
     *     <li>Check whether the sender and recipient exist</li>
     *     <li>Check if the sender has sufficient balance</li>
     *     <li>Update the balances of sender and recipient</li>
     *     <li>Create and save a TransactionRecord</li>
     * </ul>
     *
     * <p>If the transaction is invalid (sender/recipient not found or insufficient funds),
     * the database is not modified and an empty Optional is returned.
     *
     * <p>This method executes within a transaction. System exceptions will trigger
     * a rollback of database changes.
     *
     * @param t the Transaction object to be processed
     * @return an Optional containing the TransactionRecord if processed successfully,
     *         or Optional.empty() if the transaction is invalid
     */
    @Transactional
    public Optional<TransactionRecord> processTransaction(Transaction t) {
        try {
            // validation - sender/recipient exists
            UserRecord sender = userRepository.findById(t.getSenderId())
                    .orElseThrow(() -> new InvalidTransactionException("Sender not found"));
            UserRecord recipient = userRepository.findById(t.getRecipientId())
                    .orElseThrow(() -> new InvalidTransactionException("Recipient not found"));
            // validation - sender have enough balance
            float amount = t.getAmount();
            if (sender.getBalance() < amount) {
                throw new InvalidTransactionException("Sender has insufficient funds");
            }

            // process balance change
            sender.setBalance(sender.getBalance() - amount);
            recipient.setBalance(recipient.getBalance() + amount);

            // fetch and apply incentive if applicable
            float incentiveAmount = fetchIncentiveAmount(t);
            recipient.setBalance(recipient.getBalance() + incentiveAmount);

            // save sender's and recipient's balance
            userRepository.save(sender);
            userRepository.save(recipient);

            // save transaction
            TransactionRecord transactionRecord = new TransactionRecord(sender, recipient, amount, incentiveAmount);
            transactionRecordRepository.save(transactionRecord);
            logger.info("Transaction processed successfully: {}", transactionRecord);
            return Optional.of(transactionRecord);
        } catch (InvalidTransactionException e) {
            // Discard transaction
            logger.warn("Transaction failed: {} -> {} | amount {} | reason: {}",
                    t.getSenderId(), t.getRecipientId(), t.getAmount(), e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Fetches the incentive amount for a given transaction from the Incentive API.
     *
     * <p>Business logic:
     * <ul>
     *     <li>Send a POST request with the {@link Transaction} object to the Incentive API</li>
     *     <li>Deserialize the response into an {@link Incentive} object</li>
     *     <li>Return the incentive amount (>= 0) or 0 if no incentive is returned</li>
     * </ul>
     *
     * <p>If the API call fails or returns null, a warning is logged and 0 is returned.
     *
     * @param transaction the transaction to send to the Incentive API
     * @return the incentive amount to apply to the recipient
     */
    private float fetchIncentiveAmount(Transaction transaction) {
        try {
            logger.info("Sending transaction to incentive API: {} -> {} | amount: {}", 
                transaction.getSenderId(), transaction.getRecipientId(), transaction.getAmount());
            
            Incentive incentive = restTemplate.postForObject(incentiveApiUrl, transaction, Incentive.class);
            
            logger.info("Raw response from incentive API: {}", incentive);
            
            if (incentive != null && incentive.getAmount() >= 0) {
                logger.info("Fetched incentive for transaction {} -> {}: {}",
                        transaction.getSenderId(), transaction.getRecipientId(), incentive.getAmount());
                return incentive.getAmount();
            } else {
                logger.info("No incentive returned for transaction {} -> {}",
                        transaction.getSenderId(), transaction.getRecipientId());
                return 0f;
            }
        } catch (Exception e) {
            logger.warn("Failed to fetch incentive for transaction {} -> {}: {}",
                    transaction.getSenderId(), transaction.getRecipientId(), e.getMessage());
            logger.warn("Exception details:", e);
            return 0f;
        }
    }
}
