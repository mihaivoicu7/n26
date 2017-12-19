package com.n26.mihai.coding.challenge.service.impl;

import com.n26.mihai.coding.challenge.dto.StatisticsDTO;
import com.n26.mihai.coding.challenge.dto.TransactionDTO;
import com.n26.mihai.coding.challenge.enums.TransactionStatus;
import com.n26.mihai.coding.challenge.properties.ApplicationProperties;
import com.n26.mihai.coding.challenge.service.StatisticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

@RequiredArgsConstructor
@Service
@Slf4j
public class StatisticsServiceImpl implements StatisticsService{

    private final ApplicationProperties applicationProperties;

    private final Map<Long, TransactionDTO> transactionMap = new ConcurrentHashMap<>();

    private final SortedSet<BigDecimal> transactionValues = new TreeSet<>();

    private final Map<BigDecimal, Integer> duplicateValuesCount = new HashMap<>();

    private final StatisticsDTO statisticsDTO = new StatisticsDTO(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.valueOf(Double.MIN_VALUE), BigDecimal.valueOf(Double.MAX_VALUE), 0l);

    private StatisticsDTO statisticsCopy = new StatisticsDTO(BigDecimal.ZERO, BigDecimal.ZERO, null, null, 0l);

    private final AtomicLong transactionIdCount = new AtomicLong();

    private final ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();

    @Override
    public TransactionStatus createTransaction(TransactionDTO transactionDTO) {
        Date currentDate = new Date();
        Date lastMinute = new Date();
        lastMinute.setTime(currentDate.getTime() - applicationProperties.getTransactionLifeMillis());
        if(transactionDTO.getTimestamp().after(currentDate)) {
            return TransactionStatus.FUTURE;
        }
        if(transactionDTO.getTimestamp().before(lastMinute)) {
            return TransactionStatus.OLD;
        }
        transactionDTO.setAmount(transactionDTO.getAmount().setScale(applicationProperties.getDecimalScale(), BigDecimal.ROUND_HALF_EVEN));
        Runnable updateStatisticsAndCacheRunnable = () -> {
            Long transactionId = this.transactionIdCount.incrementAndGet();
            scheduleTaskRemoval(transactionId, transactionDTO.getTimestamp().getTime()-lastMinute.getTime());
            updateStatisticsAndCacheNewTransaction(transactionId, transactionDTO);
        };
        Thread updateStatisticsAndCacheThread = new Thread(updateStatisticsAndCacheRunnable);
        updateStatisticsAndCacheThread.start();
        return TransactionStatus.CREATED;
    }

    @Override
    public StatisticsDTO getStatistics() {
        return statisticsCopy;
    }

    private void scheduleTaskRemoval(Long transactionId, Long millis) {
        Runnable updateStatisticsAndCacheOldTransRunnable = () -> {
            updateStatisticsAndCacheOldTransaction(transactionId);
        };
        executorService.schedule(updateStatisticsAndCacheOldTransRunnable, millis, TimeUnit.MILLISECONDS);
    };

    private void updateStatisticsAndCacheNewTransaction(Long transactionId, TransactionDTO transactionDTO) {
        this.transactionMap.put(transactionId, transactionDTO);
        synchronized (statisticsDTO) {
            Integer duplicateValueCount = this.duplicateValuesCount.get(transactionDTO.getAmount());
            if (duplicateValueCount == null) {
                this.transactionValues.add(transactionDTO.getAmount());
                this.duplicateValuesCount.put(transactionDTO.getAmount(), 1);
            }
            else {
                this.duplicateValuesCount.replace(transactionDTO.getAmount(), duplicateValueCount+1);
            }
            Long newCount = this.statisticsDTO.getCount() + 1;
            BigDecimal newSum = this.statisticsDTO.getSum().add(transactionDTO.getAmount());
            this.statisticsDTO.setCount(newCount);
            this.statisticsDTO.setAvg(newSum.divide(BigDecimal.valueOf(newCount), applicationProperties.getDecimalScale(), BigDecimal.ROUND_HALF_EVEN));
            this.statisticsDTO.setSum(newSum);
            this.statisticsDTO.setMin(this.transactionValues.first());
            this.statisticsDTO.setMax(this.transactionValues.last());
            this.statisticsCopy = new StatisticsDTO(this.statisticsDTO.getSum(), this.statisticsDTO.getAvg(), this.statisticsDTO.getMax(), this.statisticsDTO.getMin(),this.statisticsDTO.getCount());
            this.transactionValues.add(transactionDTO.getAmount());
            log.info("Transaction with date {} and amount {} succesfully processed.", transactionDTO.getTimestamp(), transactionDTO.getAmount());
        }
    }

    private void updateStatisticsAndCacheOldTransaction(Long transactionId) {
        TransactionDTO transactionDTO = this.transactionMap.remove(transactionId);
        synchronized (statisticsDTO) {
            Long newCount = this.statisticsDTO.getCount() - 1;
            BigDecimal newSum = this.statisticsDTO.getSum().subtract(transactionDTO.getAmount());
            Integer duplicateValueCount = this.duplicateValuesCount.get(transactionDTO.getAmount());
            if(duplicateValueCount>1){
                this.duplicateValuesCount.replace(transactionDTO.getAmount(), duplicateValueCount-1);
            }
            else {
                this.duplicateValuesCount.remove(transactionDTO.getAmount());
                this.transactionValues.remove(transactionDTO.getAmount());
            }
            this.statisticsDTO.setCount(newCount);
            if(newCount>0) {
                this.statisticsDTO.setSum(newSum);
                this.statisticsDTO.setAvg(newSum.divide(BigDecimal.valueOf(newCount), applicationProperties.getDecimalScale(), BigDecimal.ROUND_HALF_EVEN));
                this.statisticsDTO.setMin(this.transactionValues.first());
                this.statisticsDTO.setMax(this.transactionValues.last());
            }
            else {
                this.statisticsDTO.setSum(BigDecimal.ZERO);
                this.statisticsDTO.setAvg(BigDecimal.ZERO);
                this.statisticsDTO.setMax(null);
                this.statisticsDTO.setMin(null);
            }
            this.statisticsCopy = new StatisticsDTO(this.statisticsDTO.getSum(), this.statisticsDTO.getAvg(), this.statisticsDTO.getMax(), this.statisticsDTO.getMin(), this.statisticsDTO.getCount());
            log.info("Transaction with date {} and amount {} succesfully removed from statistics because of old age.", transactionDTO.getTimestamp(), transactionDTO.getAmount());
        }
    }

}
