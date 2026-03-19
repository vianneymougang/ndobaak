package com.ndobaak.backend.subscription.dto;

import com.ndobaak.backend.common.enums.MobileMoneyOperator;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SubscribeRequest {

    @NotNull(message = "L'opérateur Mobile Money est requis.")
    private MobileMoneyOperator operator;

    @NotBlank(message = "Le numéro Mobile Money est requis.")
    @Pattern(regexp = "^[0-9]{9,15}$", message = "Le numéro Mobile Money doit contenir entre 9 et 15 chiffres.")
    private String phoneNumber;
}
