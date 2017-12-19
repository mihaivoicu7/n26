package com.n26.mihai.coding.challenge;

import com.n26.mihai.coding.challenge.dto.StatisticsDTO;
import com.n26.mihai.coding.challenge.dto.TransactionDTO;
import com.n26.mihai.coding.challenge.properties.ApplicationProperties;
import com.n26.mihai.coding.challenge.service.StatisticsService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;

import java.math.BigDecimal;
import java.util.Date;
import java.util.DoubleSummaryStatistics;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class StatisticsTest {

    @Autowired
    private StatisticsService statisticsService;

    @Autowired
    private ApplicationProperties applicationProperties;

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    public void testEmptyStatistics() {
        StatisticsDTO emptyStatistics = this.restTemplate.getForObject("/statistics", StatisticsDTO.class);
        assertThat(emptyStatistics).isNotNull();
        assertThat(emptyStatistics.getCount()).isEqualTo(0);
        assertThat(emptyStatistics.getSum()).isEqualTo(BigDecimal.ZERO);
        assertThat(emptyStatistics.getAvg()).isEqualTo(BigDecimal.ZERO);
        assertThat(emptyStatistics.getMin()).isNull();
        assertThat(emptyStatistics.getMax()).isNull();
    }

    @Test
    public void testOldTransactionCreation() {
        //given
        TransactionDTO transactionDTO = new TransactionDTO();
        transactionDTO.setAmount(BigDecimal.ZERO);
        Date transDate = new Date();
        transDate.setTime(transDate.getTime()-applicationProperties.getTransactionLifeMillis());
        transactionDTO.setTimestamp(transDate);
        //when
        ResponseEntity<String> response = this.restTemplate.postForEntity("/transactions", transactionDTO, String.class);
        //then
        assertThat(response.getBody()).isNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    public void testTransactionCreation() {
        //given
        this.applicationProperties.setTransactionLifeMillis(10000l);
        TransactionDTO transactionDTO = new TransactionDTO();
        transactionDTO.setAmount(BigDecimal.ZERO);
        transactionDTO.setTimestamp(new Date());
        //when
        ResponseEntity<String> response = this.restTemplate.postForEntity("/transactions", transactionDTO, String.class);
        //then
        assertThat(response.getBody()).isNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    @Test
    public void testStatisticsDuplicateTransaction() throws InterruptedException {
        //given
        this.applicationProperties.setTransactionLifeMillis(10000l);
        BigDecimal transactionValue = new BigDecimal(15.233).setScale(applicationProperties.getDecimalScale(), BigDecimal.ROUND_HALF_EVEN);
        TransactionDTO transactionDTO = new TransactionDTO();
        transactionDTO.setAmount(transactionValue);
        transactionDTO.setTimestamp(new Date());
        //when
        this.restTemplate.postForEntity("/transactions", transactionDTO, String.class);
        this.restTemplate.postForEntity("/transactions", transactionDTO, String.class);
        Thread.sleep(150l);
        StatisticsDTO statistics = this.restTemplate.getForObject("/statistics", StatisticsDTO.class);
        //then
        assertThat(statistics).isNotNull();
        assertThat(statistics.getCount()).isEqualTo(2);
        assertThat(statistics.getSum()).isEqualTo(transactionValue.add(transactionValue));
        assertThat(statistics.getAvg()).isEqualTo(transactionValue);
        assertThat(statistics.getMin()).isEqualTo(transactionValue);
        assertThat(statistics.getMax()).isEqualTo(transactionValue);
    }

    @Test
    public void testStatisticsMultipleTransactions() throws InterruptedException {
        //given
        this.applicationProperties.setTransactionLifeMillis(10000l);
        BigDecimal transactionValue1 = new BigDecimal(10).setScale(applicationProperties.getDecimalScale(), BigDecimal.ROUND_HALF_EVEN);
        TransactionDTO transactionDTO1 = new TransactionDTO();
        transactionDTO1.setAmount(transactionValue1);
        transactionDTO1.setTimestamp(new Date());

        BigDecimal transactionValue2 = new BigDecimal(20).setScale(applicationProperties.getDecimalScale(), BigDecimal.ROUND_HALF_EVEN);
        TransactionDTO transactionDTO2 = new TransactionDTO();
        transactionDTO2.setAmount(transactionValue2);
        transactionDTO2.setTimestamp(new Date());

        BigDecimal transactionValue3 = new BigDecimal(30).setScale(applicationProperties.getDecimalScale(), BigDecimal.ROUND_HALF_EVEN);
        TransactionDTO transactionDTO3 = new TransactionDTO();
        transactionDTO3.setAmount(transactionValue3);
        transactionDTO3.setTimestamp(new Date());
        //when
        this.restTemplate.postForEntity("/transactions", transactionDTO1, String.class);
        this.restTemplate.postForEntity("/transactions", transactionDTO2, String.class);
        this.restTemplate.postForEntity("/transactions", transactionDTO3, String.class);
        Thread.sleep(150l);
        StatisticsDTO statistics = this.restTemplate.getForObject("/statistics", StatisticsDTO.class);
        //then
        assertThat(statistics).isNotNull();
        assertThat(statistics.getCount()).isEqualTo(3);
        assertThat(statistics.getSum()).isEqualTo(transactionValue1.add(transactionValue2).add(transactionValue3));
        assertThat(statistics.getAvg()).isEqualTo(transactionValue1.add(transactionValue2).add(transactionValue3).divide(new BigDecimal(3)));
        assertThat(statistics.getMin()).isEqualTo(transactionValue1);
        assertThat(statistics.getMax()).isEqualTo(transactionValue3);
    }

    @Test
    public void testOldTransactionsRemovalSingleTransaction() throws InterruptedException {
        //given
        this.applicationProperties.setTransactionLifeMillis(100l);
        BigDecimal transactionValue1 = new BigDecimal(10).setScale(applicationProperties.getDecimalScale(), BigDecimal.ROUND_HALF_EVEN);
        TransactionDTO transactionDTO1 = new TransactionDTO();
        transactionDTO1.setAmount(transactionValue1);
        transactionDTO1.setTimestamp(new Date());

        //when
        this.restTemplate.postForEntity("/transactions", transactionDTO1, String.class);
        Thread.sleep(150l);
        StatisticsDTO statistics = this.restTemplate.getForObject("/statistics", StatisticsDTO.class);
        //then
        assertThat(statistics).isNotNull();
        assertThat(statistics.getCount()).isEqualTo(0);
        assertThat(statistics.getSum()).isEqualTo(BigDecimal.ZERO);
        assertThat(statistics.getAvg()).isEqualTo(BigDecimal.ZERO);
        assertThat(statistics.getMin()).isNull();
        assertThat(statistics.getMax()).isNull();
    }

    @Test
    public void testOldTransactionsRemovalMultipleTransactions() throws InterruptedException {
        //given
        this.applicationProperties.setTransactionLifeMillis(100l);
        BigDecimal transactionValue1 = new BigDecimal(10).setScale(applicationProperties.getDecimalScale(), BigDecimal.ROUND_HALF_EVEN);
        TransactionDTO transactionDTO1 = new TransactionDTO();
        transactionDTO1.setAmount(transactionValue1);
        transactionDTO1.setTimestamp(new Date());
        //when
        for(int i=0;i<5;i++) {
            this.restTemplate.postForEntity("/transactions", transactionDTO1, String.class);
        }
        Thread.sleep(150l);
        StatisticsDTO statistics = this.restTemplate.getForObject("/statistics", StatisticsDTO.class);
        //then
        assertThat(statistics).isNotNull();
        assertThat(statistics.getCount()).isEqualTo(0);
        assertThat(statistics.getSum()).isEqualTo(BigDecimal.ZERO);
        assertThat(statistics.getAvg()).isEqualTo(BigDecimal.ZERO);
        assertThat(statistics.getMin()).isNull();
        assertThat(statistics.getMax()).isNull();
    }

    @Test
    public void testStatisticsMultipleTransactionsWithRandomValues() throws InterruptedException {
        //given
        this.applicationProperties.setTransactionLifeMillis(10000l);
        Date transactionDate = new Date();
        Random random = new Random();
        List<Double> transactionValues = random.doubles(50, 0.0,1000.0).boxed().collect(Collectors.toList());
        DoubleSummaryStatistics doubleStatistics = transactionValues.stream().collect(Collectors.summarizingDouble(Double::doubleValue));
        //when
        transactionValues.stream().parallel().forEach(transValue-> {
            TransactionDTO transactionDTO = new TransactionDTO();
            transactionDTO.setTimestamp(transactionDate);
            transactionDTO.setAmount(BigDecimal.valueOf(transValue).setScale(applicationProperties.getDecimalScale(), BigDecimal.ROUND_HALF_EVEN));
            this.restTemplate.postForEntity("/transactions", transactionDTO, String.class);
        });
        Thread.sleep(500l);
        StatisticsDTO statistics = this.restTemplate.getForObject("/statistics", StatisticsDTO.class);
        //then
        assertThat(statistics).isNotNull();
        assertThat(statistics.getCount()).isEqualTo(50);
        assertThat(statistics.getSum().setScale(1, BigDecimal.ROUND_HALF_EVEN)).isEqualTo(BigDecimal.valueOf(doubleStatistics.getSum()).setScale(1, BigDecimal.ROUND_HALF_EVEN));
        assertThat(statistics.getAvg().setScale(1, BigDecimal.ROUND_HALF_EVEN)).isEqualTo(BigDecimal.valueOf(doubleStatistics.getAverage()).setScale(1, BigDecimal
                .ROUND_HALF_EVEN));
        assertThat(statistics.getMin().setScale(1, BigDecimal.ROUND_HALF_EVEN)).isEqualTo(BigDecimal.valueOf(doubleStatistics.getMin()).setScale(1, BigDecimal.ROUND_HALF_EVEN));
        assertThat(statistics.getMax().setScale(1, BigDecimal.ROUND_HALF_EVEN)).isEqualTo(BigDecimal.valueOf(doubleStatistics.getMax()).setScale(1, BigDecimal.ROUND_HALF_EVEN));
    }

}
