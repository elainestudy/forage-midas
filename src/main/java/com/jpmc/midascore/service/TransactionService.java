package com.jpmc.midascore.service;

import com.jpmc.midascore.entity.TransactionRecord;
import com.jpmc.midascore.entity.UserRecord;
import com.jpmc.midascore.exception.InvalidTransactionException;
import com.jpmc.midascore.foundation.Transaction;
import com.jpmc.midascore.repository.TransactionRecordRepository;
import com.jpmc.midascore.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class TransactionService {
    static final Logger logger = LoggerFactory.getLogger(TransactionService.class);

    private final UserRepository userRepository;
    private final TransactionRecordRepository transactionRecordRepository;

    public TransactionService(UserRepository userRepository,
                              TransactionRecordRepository transactionRecordRepository) {
        this.userRepository = userRepository;
        this.transactionRecordRepository = transactionRecordRepository;
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
            userRepository.save(sender);
            userRepository.save(recipient);

            // save transaction
            TransactionRecord transactionRecord = new TransactionRecord(sender, recipient, amount);
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
}
