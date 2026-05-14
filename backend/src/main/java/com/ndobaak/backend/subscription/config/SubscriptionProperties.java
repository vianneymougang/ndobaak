package com.ndobaak.backend.subscription.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@ConfigurationProperties(prefix = "app.subscription")
@Getter
@Setter
public class SubscriptionProperties {

    private BigDecimal amount = new BigDecimal("100000");
}
