package com.ndobaak.backend.notification.controller;

import com.ndobaak.backend.auth.service.CustomOAuth2UserService;
import com.ndobaak.backend.common.enums.NotificationType;
import com.ndobaak.backend.common.exception.ResourceNotFoundException;
import com.ndobaak.backend.common.security.JwtAuthenticationFilter;
import com.ndobaak.backend.common.security.OAuth2AuthenticationFailureHandler;
import com.ndobaak.backend.common.security.OAuth2AuthenticationSuccessHandler;
import com.ndobaak.backend.notification.dto.NotificationResponse;
import com.ndobaak.backend.notification.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(NotificationController.class)
@AutoConfigureMockMvc(addFilters = false)
class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private NotificationService notificationService;
    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;
    @MockitoBean
    private CustomOAuth2UserService customOAuth2UserService;
    @MockitoBean
    private OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler;
    @MockitoBean
    private OAuth2AuthenticationFailureHandler oAuth2AuthenticationFailureHandler;

    private NotificationResponse buildNotificationResponse(Long id) {
        return NotificationResponse.builder()
                .id(id)
                .type(NotificationType.BOOKING_CREATED)
                .title("Nouvelle réservation")
                .message("Une réservation a été créée.")
                .read(false)
                .createdAt(LocalDateTime.of(2026, 3, 18, 10, 0))
                .build();
    }

    @Test
    @WithMockUser
    void getNotificationsShouldReturn200() throws Exception {
        NotificationResponse response = buildNotificationResponse(1L);
        Page<NotificationResponse> page = new PageImpl<>(List.of(response), PageRequest.of(0, 20), 1);

        when(notificationService.getUserNotifications(any())).thenReturn(page);

        mockMvc.perform(get("/notifications"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].type").value("BOOKING_CREATED"))
                .andExpect(jsonPath("$.content[0].read").value(false))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @WithMockUser
    void getNotificationsShouldRespectPagination() throws Exception {
        Page<NotificationResponse> emptyPage = new PageImpl<>(List.of(), PageRequest.of(5, 10), 0);
        when(notificationService.getUserNotifications(any())).thenReturn(emptyPage);

        mockMvc.perform(get("/notifications").param("page", "5").param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty());
    }

    @Test
    @WithMockUser
    void getUnreadCountShouldReturn200() throws Exception {
        when(notificationService.getUnreadCount()).thenReturn(3L);

        mockMvc.perform(get("/notifications/unread-count"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(3));
    }

    @Test
    @WithMockUser
    void markAsReadShouldReturn204() throws Exception {
        doNothing().when(notificationService).markAsRead(1L);

        mockMvc.perform(put("/notifications/1/read"))
                .andExpect(status().isNoContent());

        verify(notificationService).markAsRead(1L);
    }

    @Test
    @WithMockUser
    void markAsReadShouldReturn404WhenNotFound() throws Exception {
        doThrow(new ResourceNotFoundException("Notification", "id", 999L))
                .when(notificationService).markAsRead(999L);

        mockMvc.perform(put("/notifications/999/read"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser
    void markAllAsReadShouldReturn204() throws Exception {
        doNothing().when(notificationService).markAllAsRead();

        mockMvc.perform(put("/notifications/read-all"))
                .andExpect(status().isNoContent());

        verify(notificationService).markAllAsRead();
    }
}
