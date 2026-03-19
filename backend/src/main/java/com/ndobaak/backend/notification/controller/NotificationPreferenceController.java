package com.ndobaak.backend.notification.controller;

import com.ndobaak.backend.auth.service.AuthService;
import com.ndobaak.backend.common.enums.NotificationType;
import com.ndobaak.backend.notification.dto.NotificationTypePreferenceResponse;
import com.ndobaak.backend.notification.dto.UpdatePreferenceRequest;
import com.ndobaak.backend.notification.service.NotificationPreferenceService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/notifications/preferences")
@PreAuthorize("isAuthenticated()")
public class NotificationPreferenceController {

    private final NotificationPreferenceService preferenceService;
    private final AuthService authService;

    public NotificationPreferenceController(NotificationPreferenceService preferenceService,
                                             AuthService authService) {
        this.preferenceService = preferenceService;
        this.authService = authService;
    }

    @GetMapping
    public ResponseEntity<List<NotificationTypePreferenceResponse>> getPreferences() {
        Long userId = authService.getAuthenticatedUser().getId();
        return ResponseEntity.ok(preferenceService.getPreferences(userId));
    }

    @PutMapping("/{type}")
    public ResponseEntity<NotificationTypePreferenceResponse> updatePreference(
            @PathVariable NotificationType type,
            @RequestBody UpdatePreferenceRequest request) {
        Long userId = authService.getAuthenticatedUser().getId();
        return ResponseEntity.ok(preferenceService.updatePreference(userId, type, request));
    }
}
