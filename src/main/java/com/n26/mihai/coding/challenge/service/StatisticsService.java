package com.n26.mihai.coding.challenge.service;

import com.n26.mihai.coding.challenge.dto.StatisticsDTO;
import com.n26.mihai.coding.challenge.dto.TransactionDTO;
import com.n26.mihai.coding.challenge.enums.TransactionStatus;

public interface StatisticsService {

    public TransactionStatus createTransaction(TransactionDTO transactionDTO);

    public StatisticsDTO getStatistics();

}
