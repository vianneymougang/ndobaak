package com.ndobaak.backend.payment.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ndobaak.backend.auth.service.CustomOAuth2UserService;
import com.ndobaak.backend.common.enums.MobileMoneyOperator;
import com.ndobaak.backend.common.enums.PaymentStatus;
import com.ndobaak.backend.common.enums.PaymentType;
import com.ndobaak.backend.common.enums.ScheduleFrequency;
import com.ndobaak.backend.common.exception.BusinessException;
import com.ndobaak.backend.common.exception.ForbiddenException;
import com.ndobaak.backend.common.exception.ResourceNotFoundException;
import com.ndobaak.backend.common.security.JwtAuthenticationFilter;
import com.ndobaak.backend.common.security.OAuth2AuthenticationFailureHandler;
import com.ndobaak.backend.common.security.OAuth2AuthenticationSuccessHandler;
import com.ndobaak.backend.payment.dto.InitiatePaymentRequest;
import com.ndobaak.backend.payment.dto.PayInstallmentRequest;
import com.ndobaak.backend.payment.dto.PaymentResponse;
import com.ndobaak.backend.payment.dto.PaymentScheduleResponse;
import com.ndobaak.backend.payment.dto.ReceiptResponse;
import com.ndobaak.backend.payment.service.PaymentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PaymentController.class)
@AutoConfigureMockMvc(addFilters = false)
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private PaymentService paymentService;
    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;
    @MockitoBean
    private CustomOAuth2UserService customOAuth2UserService;
    @MockitoBean
    private OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler;
    @MockitoBean
    private OAuth2AuthenticationFailureHandler oAuth2AuthenticationFailureHandler;

    private PaymentResponse buildPaymentResponse(Long id, PaymentStatus status) {
        return PaymentResponse.builder()
                .id(id)
                .amount(new BigDecimal("300000"))
                .status(status)
                .paymentType(PaymentType.INTEGRAL)
                .operator(MobileMoneyOperator.MTN_MOMO)
                .createdAt(LocalDateTime.of(2026, 3, 18, 10, 0))
                .build();
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    void initiatePaymentShouldReturn201() throws Exception {
        InitiatePaymentRequest request = new InitiatePaymentRequest();
        request.setBookingId(1L);
        request.setPaymentType(PaymentType.INTEGRAL);
        request.setOperator(MobileMoneyOperator.MTN_MOMO);
        request.setPhoneNumber("+237670000000");

        PaymentResponse response = buildPaymentResponse(1L, PaymentStatus.PROCESSING);
        response.setAuthorizationUrl("https://pay.example.com");

        when(paymentService.initiatePayment(any())).thenReturn(response);

        mockMvc.perform(post("/payments/initiate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("PROCESSING"))
                .andExpect(jsonPath("$.authorizationUrl").value("https://pay.example.com"));
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    void initiatePaymentShouldReturn400WhenMissingFields() throws Exception {
        mockMvc.perform(post("/payments/initiate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    void initiatePaymentShouldReturn400OnBusinessError() throws Exception {
        InitiatePaymentRequest request = new InitiatePaymentRequest();
        request.setBookingId(1L);
        request.setPaymentType(PaymentType.INTEGRAL);
        request.setOperator(MobileMoneyOperator.MTN_MOMO);
        request.setPhoneNumber("+237670000000");

        when(paymentService.initiatePayment(any()))
                .thenThrow(new BusinessException("Réservation non confirmée"));

        mockMvc.perform(post("/payments/initiate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void webhookShouldReturn200OnValidPayload() throws Exception {
        doNothing().when(paymentService).handleWebhook(eq("notchpay"), any(), any());

        mockMvc.perform(post("/payments/webhook/notchpay")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Notchpay-Signature", "valid-sig")
                        .content("{\"event\":\"payment.complete\",\"reference\":\"ref-123\"}"))
                .andExpect(status().isOk());

        verify(paymentService).handleWebhook(eq("notchpay"), any(), eq("valid-sig"));
    }

    @Test
    void webhookShouldReturn400OnInvalidSignature() throws Exception {
        doThrow(new BusinessException("Signature webhook invalide."))
                .when(paymentService).handleWebhook(eq("notchpay"), any(), any());

        mockMvc.perform(post("/payments/webhook/notchpay")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Notchpay-Signature", "bad-sig")
                        .content("{\"event\":\"payment.complete\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser
    void getPaymentsByBookingShouldReturn200() throws Exception {
        PaymentResponse response = buildPaymentResponse(1L, PaymentStatus.COMPLETED);
        when(paymentService.getPaymentsByBooking(1L)).thenReturn(List.of(response));

        mockMvc.perform(get("/payments/booking/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].status").value("COMPLETED"));
    }

    @Test
    @WithMockUser
    void getPaymentsByBookingShouldReturn403WhenUnauthorized() throws Exception {
        when(paymentService.getPaymentsByBooking(1L))
                .thenThrow(new ForbiddenException("Accès interdit"));

        mockMvc.perform(get("/payments/booking/1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser
    void getTransactionHistoryShouldReturn200() throws Exception {
        PaymentResponse response = buildPaymentResponse(1L, PaymentStatus.COMPLETED);
        Page<PaymentResponse> page = new PageImpl<>(List.of(response), PageRequest.of(0, 20), 1);

        when(paymentService.getTransactionHistory(any())).thenReturn(page);

        mockMvc.perform(get("/payments/history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @WithMockUser
    void getReceiptShouldReturn200() throws Exception {
        ReceiptResponse receipt = ReceiptResponse.builder()
                .transactionId("ref-123")
                .rentAmount(new BigDecimal("300000"))
                .status(PaymentStatus.COMPLETED)
                .build();

        when(paymentService.getReceipt(1L)).thenReturn(receipt);

        mockMvc.perform(get("/payments/1/receipt"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactionId").value("ref-123"));
    }

    @Test
    @WithMockUser
    void getReceiptShouldReturn400WhenNotCompleted() throws Exception {
        when(paymentService.getReceipt(1L))
                .thenThrow(new BusinessException("Reçu non disponible"));

        mockMvc.perform(get("/payments/1/receipt"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    void payInstallmentShouldReturn201() throws Exception {
        PayInstallmentRequest request = new PayInstallmentRequest();
        request.setOperator(MobileMoneyOperator.MTN_MOMO);
        request.setPhoneNumber("+237670000000");

        when(paymentService.payInstallment(eq(1L), any()))
                .thenReturn(buildPaymentResponse(2L, PaymentStatus.PROCESSING));

        mockMvc.perform(post("/payments/installments/1/pay")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(2));
    }

    @Test
    @WithMockUser
    void getPaymentScheduleShouldReturn200() throws Exception {
        PaymentScheduleResponse schedule = PaymentScheduleResponse.builder()
                .id(1L)
                .totalAmount(new BigDecimal("300000"))
                .frequency(ScheduleFrequency.MONTHLY)
                .numberOfInstallments(6)
                .build();

        when(paymentService.getPaymentSchedule(1L)).thenReturn(schedule);

        mockMvc.perform(get("/payments/schedule/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalAmount").value(300000))
                .andExpect(jsonPath("$.numberOfInstallments").value(6));
    }

    @Test
    @WithMockUser
    void getPaymentScheduleShouldReturn404WhenNotFound() throws Exception {
        when(paymentService.getPaymentSchedule(999L))
                .thenThrow(new ResourceNotFoundException("Aucun échéancier trouvé"));

        mockMvc.perform(get("/payments/schedule/999"))
                .andExpect(status().isNotFound());
    }
}
