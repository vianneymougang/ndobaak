package com.ndobaak.backend.payment.gateway;

import com.ndobaak.backend.common.enums.PaymentChannel;
import com.ndobaak.backend.common.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PaymentGatewayRouter {

    private static final Logger log = LoggerFactory.getLogger(PaymentGatewayRouter.class);

    private final List<PaymentGateway> gateways;
    private final PaymentProperties properties;

    public PaymentGatewayRouter(List<PaymentGateway> gateways, PaymentProperties properties) {
        this.gateways = gateways;
        this.properties = properties;
        log.info("Payment gateways registered: {}",
                gateways.stream().map(PaymentGateway::getProviderName).toList());
    }

    public PaymentGateway resolveGateway(PaymentChannel channel) {
        PaymentGateway primary = findByName(properties.getPrimaryGateway());
        if (primary != null && primary.supports(channel) && primary.isAvailable()) {
            return primary;
        }

        log.warn("Primary gateway '{}' unavailable or unsupported for {}. Trying fallback...",
                properties.getPrimaryGateway(), channel);

        return gateways.stream()
                .filter(g -> g.supports(channel))
                .filter(PaymentGateway::isAvailable)
                .findFirst()
                .orElseThrow(() -> new BusinessException(
                        "Aucun service de paiement disponible pour le moment. Veuillez réessayer plus tard."));
    }

    public PaymentGateway resolveGatewayByProvider(String providerName) {
        PaymentGateway gateway = findByName(providerName);
        if (gateway == null) {
            throw new BusinessException("Service de paiement inconnu : " + providerName);
        }
        return gateway;
    }

    private PaymentGateway findByName(String name) {
        return gateways.stream()
                .filter(g -> g.getProviderName().equalsIgnoreCase(name))
                .findFirst()
                .orElse(null);
    }
}
