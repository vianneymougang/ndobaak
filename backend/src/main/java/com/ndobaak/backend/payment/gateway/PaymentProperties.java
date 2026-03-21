package com.ndobaak.backend.payment.gateway;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@ConfigurationProperties(prefix = "app.payment")
@Getter
@Setter
public class PaymentProperties {

    private BigDecimal commissionRate = new BigDecimal("0.05");
    private BigDecimal installmentThreshold = new BigDecimal("500000");
    private String primaryGateway = "notchpay";
    private String frontendCallbackUrl = "http://localhost:4200/payments/callback";
}
