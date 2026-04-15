package com.ndobaak.backend.subscription.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ndobaak.backend.auth.service.CustomOAuth2UserService;
import com.ndobaak.backend.common.enums.PaymentChannel;
import com.ndobaak.backend.common.enums.SubscriptionStatus;
import com.ndobaak.backend.common.exception.BusinessException;
import com.ndobaak.backend.common.exception.ResourceNotFoundException;
import com.ndobaak.backend.common.security.JwtAuthenticationFilter;
import com.ndobaak.backend.common.security.OAuth2AuthenticationFailureHandler;
import com.ndobaak.backend.common.security.OAuth2AuthenticationSuccessHandler;
import com.ndobaak.backend.subscription.dto.SubscribeRequest;
import com.ndobaak.backend.subscription.dto.SubscriptionResponse;
import com.ndobaak.backend.subscription.service.SubscriptionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SubscriptionController.class)
@AutoConfigureMockMvc(addFilters = false)
class SubscriptionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private SubscriptionService subscriptionService;
    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;
    @MockitoBean
    private CustomOAuth2UserService customOAuth2UserService;
    @MockitoBean
    private OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler;
    @MockitoBean
    private OAuth2AuthenticationFailureHandler oAuth2AuthenticationFailureHandler;

    private SubscriptionResponse buildResponse(Long id, SubscriptionStatus status) {
        return SubscriptionResponse.builder()
                .id(id)
                .status(status)
                .startDate(LocalDate.of(2026, 1, 1))
                .endDate(LocalDate.of(2027, 1, 1))
                .maxListings(20)
                .amount(BigDecimal.valueOf(100_000))
                .autoRenew(false)
                .daysRemaining(288)
                .paymentReference("ref-abc-123")
                .build();
    }

    private SubscribeRequest buildRequest() {
        SubscribeRequest request = new SubscribeRequest();
        request.setOperator(PaymentChannel.MTN_MOMO);
        request.setPhoneNumber("677000001");
        return request;
    }

    @Test
    @WithMockUser(roles = "LANDLORD")
    void subscribeShouldReturn200WithSubscriptionResponse() throws Exception {
        SubscriptionResponse response = buildResponse(1L, SubscriptionStatus.PENDING_PAYMENT);
        when(subscriptionService.subscribe(any(SubscribeRequest.class))).thenReturn(response);

        mockMvc.perform(post("/subscriptions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("PENDING_PAYMENT"))
                .andExpect(jsonPath("$.paymentReference").value("ref-abc-123"));
    }

    @Test
    @WithMockUser(roles = "LANDLORD")
    void subscribeShouldReturn400WhenAlreadyActive() throws Exception {
        when(subscriptionService.subscribe(any(SubscribeRequest.class)))
                .thenThrow(new BusinessException("Vous avez déjà un abonnement actif."));

        mockMvc.perform(post("/subscriptions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildRequest())))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "LANDLORD")
    void subscribeShouldReturn400WhenRequestBodyInvalid() throws Exception {
        SubscribeRequest invalidRequest = new SubscribeRequest();

        mockMvc.perform(post("/subscriptions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "LANDLORD")
    void getMySubscriptionShouldReturn200WhenExists() throws Exception {
        SubscriptionResponse response = buildResponse(1L, SubscriptionStatus.ACTIVE);
        when(subscriptionService.getMySubscription()).thenReturn(response);

        mockMvc.perform(get("/subscriptions/my"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    @WithMockUser(roles = "LANDLORD")
    void getMySubscriptionShouldReturn204WhenNoneExists() throws Exception {
        when(subscriptionService.getMySubscription()).thenReturn(null);

        mockMvc.perform(get("/subscriptions/my"))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(roles = "LANDLORD")
    void renewShouldReturn200WithUpdatedResponse() throws Exception {
        SubscriptionResponse response = buildResponse(1L, SubscriptionStatus.PENDING_PAYMENT);
        when(subscriptionService.renew(any(SubscribeRequest.class))).thenReturn(response);

        mockMvc.perform(put("/subscriptions/renew")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING_PAYMENT"));
    }

    @Test
    @WithMockUser(roles = "LANDLORD")
    void renewShouldReturn404WhenNoSubscriptionFound() throws Exception {
        when(subscriptionService.renew(any(SubscribeRequest.class)))
                .thenThrow(new ResourceNotFoundException("Aucun abonnement trouvé."));

        mockMvc.perform(put("/subscriptions/renew")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildRequest())))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "LANDLORD")
    void cancelAutoRenewShouldReturn200WithUpdatedResponse() throws Exception {
        SubscriptionResponse response = buildResponse(1L, SubscriptionStatus.ACTIVE);
        when(subscriptionService.cancelAutoRenew()).thenReturn(response);

        mockMvc.perform(put("/subscriptions/cancel-auto-renew"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }
}
