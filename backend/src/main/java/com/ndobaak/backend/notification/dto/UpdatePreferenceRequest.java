package com.ndobaak.backend.notification.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class UpdatePreferenceRequest {

    private Boolean emailEnabled;
    private Boolean smsEnabled;
}
