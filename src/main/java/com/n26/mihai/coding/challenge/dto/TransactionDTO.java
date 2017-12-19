package com.n26.mihai.coding.challenge.dto;

import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.Date;

@Data
public class TransactionDTO {
    @NotNull
    private BigDecimal amount;
    @NotNull
    private Date timestamp;
}
