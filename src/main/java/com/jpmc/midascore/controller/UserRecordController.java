package com.jpmc.midascore.controller;

import com.jpmc.midascore.entity.UserRecord;
import com.jpmc.midascore.foundation.Balance;
import com.jpmc.midascore.repository.UserRepository;
import com.jpmc.midascore.service.TransactionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
public class UserRecordController {
    static final Logger logger = LoggerFactory.getLogger(UserRecordController.class);

    private final UserRepository userRepository;

    public UserRecordController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/balance")
    public Balance getBalance(@RequestParam Long userId) {
        Optional<UserRecord> userRecordOptional = userRepository.findById(userId);

        if (userRecordOptional.isPresent()) {
            UserRecord userRecord = userRecordOptional.get();
            return new Balance(userRecord.getBalance());
        } else {
            return new Balance(0f);
        }
    }

}
