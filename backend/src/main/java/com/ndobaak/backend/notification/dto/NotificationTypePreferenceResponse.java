package com.ndobaak.backend.notification.dto;

import com.ndobaak.backend.common.enums.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationTypePreferenceResponse {

    private NotificationType notificationType;
    private boolean emailEnabled;
    private boolean smsEnabled;
    private boolean inAppEnabled;
    private boolean critical;
}
