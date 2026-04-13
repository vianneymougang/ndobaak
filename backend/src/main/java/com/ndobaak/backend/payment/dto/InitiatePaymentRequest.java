package com.ndobaak.backend.payment.dto;

import com.ndobaak.backend.common.enums.PaymentChannel;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class InitiatePaymentRequest {

    @NotNull(message = "L'identifiant de la réservation est requis")
    private Long bookingId;

    @NotNull(message = "L'opérateur Mobile Money est requis")
    private PaymentChannel operator;

    @NotNull(message = "Le numéro de téléphone est requis")
    @Pattern(regexp = "^\\+?237[0-9]{9}$", message = "Le numéro de téléphone doit être un numéro camerounais valide")
    private String phoneNumber;
}
