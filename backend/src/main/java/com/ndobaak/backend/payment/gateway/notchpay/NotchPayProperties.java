package com.ndobaak.backend.payment.gateway.notchpay;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.payment.notchpay")
@Getter
@Setter
public class NotchPayProperties {

    private String baseUrl = "https://api.notchpay.co";
    private String publicKey;
    private String webhookHash;
    private String callbackUrl;
    private String privateKey;
}
