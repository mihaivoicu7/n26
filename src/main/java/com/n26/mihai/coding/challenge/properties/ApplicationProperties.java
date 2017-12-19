package com.n26.mihai.coding.challenge.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

@Data
@Component
@ConfigurationProperties
public class ApplicationProperties {
    @Min(0)
    @NotNull
    private Long transactionLifeMillis;

    private Integer decimalScale = 3;
}
