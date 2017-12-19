package com.n26.mihai.coding.challenge.controller;

import com.n26.mihai.coding.challenge.dto.StatisticsDTO;
import com.n26.mihai.coding.challenge.dto.TransactionDTO;
import com.n26.mihai.coding.challenge.enums.TransactionStatus;
import com.n26.mihai.coding.challenge.service.StatisticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RequiredArgsConstructor
@RestController
@Slf4j
public class TransactionsController {

    private final StatisticsService statisticsService;

    @PostMapping("/transactions")
    public ResponseEntity<String> createTransaction(@RequestBody @Valid TransactionDTO transaction){
        log.info("Request to create new transaction with date {} and value {}", transaction.getTimestamp(), transaction.getAmount());
        TransactionStatus status = statisticsService.createTransaction(transaction);
        switch (status) {
            case CREATED: return ResponseEntity.status(HttpStatus.CREATED).body(null);
            case OLD: {log.info("Transaction date is too old."); return ResponseEntity.status(HttpStatus.NO_CONTENT).body(null);}
            case FUTURE:  {log.info("Invalid data. Transaction date is in the future"); return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Cannot create transaction in " +
                    "the future.");}
            default: return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @GetMapping("/statistics")
    public ResponseEntity<StatisticsDTO> getStatistics() {
        return ResponseEntity.ok(statisticsService.getStatistics());
    }
}
