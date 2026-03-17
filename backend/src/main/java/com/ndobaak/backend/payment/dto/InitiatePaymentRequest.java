package com.ndobaak.backend.payment.dto;

import com.ndobaak.backend.common.enums.MobileMoneyOperator;
import com.ndobaak.backend.common.enums.PaymentType;
import com.ndobaak.backend.common.enums.ScheduleFrequency;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class InitiatePaymentRequest {

    @NotNull(message = "L'identifiant de la réservation est requis")
    private Long bookingId;

    @NotNull(message = "Le type de paiement est requis")
    private PaymentType paymentType;

    @NotNull(message = "L'opérateur Mobile Money est requis")
    private MobileMoneyOperator operator;

    @NotNull(message = "Le numéro de téléphone est requis")
    @Pattern(regexp = "^\\+?237[0-9]{9}$", message = "Le numéro de téléphone doit être un numéro camerounais valide")
    private String phoneNumber;

    private ScheduleFrequency installmentFrequency;
}
