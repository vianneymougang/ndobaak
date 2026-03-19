package com.ndobaak.backend.notification.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ndobaak.backend.auth.entity.Student;
import com.ndobaak.backend.auth.service.AuthService;
import com.ndobaak.backend.auth.service.CustomOAuth2UserService;
import com.ndobaak.backend.common.enums.NotificationType;
import com.ndobaak.backend.common.exception.BusinessException;
import com.ndobaak.backend.common.security.JwtAuthenticationFilter;
import com.ndobaak.backend.common.security.OAuth2AuthenticationFailureHandler;
import com.ndobaak.backend.common.security.OAuth2AuthenticationSuccessHandler;
import com.ndobaak.backend.notification.dto.NotificationTypePreferenceResponse;
import com.ndobaak.backend.notification.dto.UpdatePreferenceRequest;
import com.ndobaak.backend.notification.service.NotificationPreferenceService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(NotificationPreferenceController.class)
@AutoConfigureMockMvc(addFilters = false)
class NotificationPreferenceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private NotificationPreferenceService preferenceService;
    @MockitoBean
    private AuthService authService;
    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;
    @MockitoBean
    private CustomOAuth2UserService customOAuth2UserService;
    @MockitoBean
    private OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler;
    @MockitoBean
    private OAuth2AuthenticationFailureHandler oAuth2AuthenticationFailureHandler;

    private Student buildAuthUser(Long id) {
        return Student.builder()
                .firstName("Alice")
                .lastName("Test")
                .email("alice@test.com")
                .build();
    }

    private NotificationTypePreferenceResponse buildPrefResponse(NotificationType type, boolean email, boolean sms, boolean critical) {
        return NotificationTypePreferenceResponse.builder()
                .notificationType(type)
                .emailEnabled(email)
                .smsEnabled(sms)
                .inAppEnabled(true)
                .critical(critical)
                .build();
    }

    @Test
    @WithMockUser
    void getPreferencesShouldReturn200WithList() throws Exception {
        Student user = buildAuthUser(1L);
        when(authService.getAuthenticatedUser()).thenReturn(user);
        when(preferenceService.getPreferences(user.getId())).thenReturn(List.of(
                buildPrefResponse(NotificationType.BOOKING_CREATED, true, false, false),
                buildPrefResponse(NotificationType.BOOKING_CONFIRMED, true, true, true)));

        mockMvc.perform(get("/notifications/preferences"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].notificationType").value("BOOKING_CREATED"))
                .andExpect(jsonPath("$[0].emailEnabled").value(true))
                .andExpect(jsonPath("$[0].critical").value(false))
                .andExpect(jsonPath("$[1].critical").value(true));
    }

    @Test
    @WithMockUser
    void getPreferencesShouldReturnEmptyList() throws Exception {
        Student user = buildAuthUser(1L);
        when(authService.getAuthenticatedUser()).thenReturn(user);
        when(preferenceService.getPreferences(user.getId())).thenReturn(List.of());

        mockMvc.perform(get("/notifications/preferences"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @WithMockUser
    void updatePreferenceShouldReturn200WithUpdatedPref() throws Exception {
        Student user = buildAuthUser(1L);
        when(authService.getAuthenticatedUser()).thenReturn(user);
        when(preferenceService.updatePreference(eq(user.getId()), eq(NotificationType.BOOKING_CREATED), any()))
                .thenReturn(buildPrefResponse(NotificationType.BOOKING_CREATED, false, true, false));

        UpdatePreferenceRequest request = new UpdatePreferenceRequest();
        request.setEmailEnabled(false);
        request.setSmsEnabled(true);

        mockMvc.perform(put("/notifications/preferences/BOOKING_CREATED")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.notificationType").value("BOOKING_CREATED"))
                .andExpect(jsonPath("$.emailEnabled").value(false))
                .andExpect(jsonPath("$.smsEnabled").value(true));
    }

    @Test
    @WithMockUser
    void updatePreferenceShouldReturn400ForCriticalType() throws Exception {
        Student user = buildAuthUser(1L);
        when(authService.getAuthenticatedUser()).thenReturn(user);
        when(preferenceService.updatePreference(eq(user.getId()), eq(NotificationType.BOOKING_CONFIRMED), any()))
                .thenThrow(new BusinessException("Les notifications critiques ne peuvent pas être modifiées."));

        UpdatePreferenceRequest request = new UpdatePreferenceRequest();
        request.setEmailEnabled(false);

        mockMvc.perform(put("/notifications/preferences/BOOKING_CONFIRMED")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser
    void updatePreferenceShouldReturn400ForInvalidTypePath() throws Exception {
        mockMvc.perform(put("/notifications/preferences/INVALID_TYPE")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }
}
