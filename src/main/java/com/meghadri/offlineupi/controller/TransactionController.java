package com.meghadri.offlineupi.controller;

import com.meghadri.offlineupi.transaction.entity.Transaction;
import com.meghadri.offlineupi.transaction.repository.TransactionRepository;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private final TransactionRepository transactionRepository;

    public TransactionController(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    @GetMapping
    public List<Transaction> recent() {
        return transactionRepository.findTop20ByOrderByCreatedAtDesc();
    }
}