package com.ndobaak.backend.booking.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ndobaak.backend.auth.service.CustomOAuth2UserService;
import com.ndobaak.backend.booking.dto.BookingResponse;
import com.ndobaak.backend.booking.dto.BookingStatusUpdateRequest;
import com.ndobaak.backend.booking.dto.CreateBookingRequest;
import com.ndobaak.backend.booking.service.BookingService;
import com.ndobaak.backend.common.enums.BookingStatus;
import com.ndobaak.backend.common.exception.BusinessException;
import com.ndobaak.backend.common.exception.ForbiddenException;
import com.ndobaak.backend.common.exception.ResourceNotFoundException;
import com.ndobaak.backend.common.security.JwtAuthenticationFilter;
import com.ndobaak.backend.common.security.OAuth2AuthenticationFailureHandler;
import com.ndobaak.backend.common.security.OAuth2AuthenticationSuccessHandler;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BookingController.class)
@AutoConfigureMockMvc(addFilters = false)
class BookingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private BookingService bookingService;
    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;
    @MockitoBean
    private CustomOAuth2UserService customOAuth2UserService;
    @MockitoBean
    private OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler;
    @MockitoBean
    private OAuth2AuthenticationFailureHandler oAuth2AuthenticationFailureHandler;

    private BookingResponse buildResponse(Long id, BookingStatus status) {
        return BookingResponse.builder()
                .id(id)
                .startDate(LocalDate.of(2026, 5, 1))
                .endDate(LocalDate.of(2026, 8, 1))
                .monthlyRent(new BigDecimal("35000"))
                .totalAmount(new BigDecimal("105000"))
                .status(status)
                .createdAt(LocalDateTime.of(2026, 3, 18, 10, 0))
                .build();
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    void createBookingShouldReturn201() throws Exception {
        CreateBookingRequest request = new CreateBookingRequest();
        request.setListingId(1L);
        request.setStartDate(LocalDate.of(2026, 5, 1));
        request.setEndDate(LocalDate.of(2026, 8, 1));

        when(bookingService.createBooking(any())).thenReturn(buildResponse(1L, BookingStatus.PENDING));

        mockMvc.perform(post("/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    void createBookingShouldReturn400WhenMissingFields() throws Exception {
        mockMvc.perform(post("/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    void createBookingShouldReturn400OnBusinessError() throws Exception {
        CreateBookingRequest request = new CreateBookingRequest();
        request.setListingId(1L);
        request.setStartDate(LocalDate.of(2026, 5, 1));
        request.setEndDate(LocalDate.of(2026, 8, 1));

        when(bookingService.createBooking(any()))
                .thenThrow(new BusinessException("Annonce non disponible"));

        mockMvc.perform(post("/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser
    void getBookingByIdShouldReturn200() throws Exception {
        when(bookingService.getBookingById(1L)).thenReturn(buildResponse(1L, BookingStatus.CONFIRMED));

        mockMvc.perform(get("/bookings/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("CONFIRMED"));
    }

    @Test
    @WithMockUser
    void getBookingByIdShouldReturn404WhenNotFound() throws Exception {
        when(bookingService.getBookingById(999L))
                .thenThrow(new ResourceNotFoundException("Booking", "id", 999L));

        mockMvc.perform(get("/bookings/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser
    void getBookingByIdShouldReturn403WhenForbidden() throws Exception {
        when(bookingService.getBookingById(1L))
                .thenThrow(new ForbiddenException("Accès interdit"));

        mockMvc.perform(get("/bookings/1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    void getMyBookingsShouldReturn200() throws Exception {
        BookingResponse response = buildResponse(1L, BookingStatus.PENDING);
        Page<BookingResponse> page = new PageImpl<>(List.of(response), PageRequest.of(0, 10), 1);

        when(bookingService.getStudentBookings(any(), any())).thenReturn(page);

        mockMvc.perform(get("/bookings/my"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    void getMyBookingsShouldFilterByStatus() throws Exception {
        Page<BookingResponse> page = new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);
        when(bookingService.getStudentBookings(eq(BookingStatus.CONFIRMED), any())).thenReturn(page);

        mockMvc.perform(get("/bookings/my").param("status", "CONFIRMED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty());
    }

    @Test
    @WithMockUser(roles = "LANDLORD")
    void getLandlordBookingsShouldReturn200() throws Exception {
        BookingResponse response = buildResponse(1L, BookingStatus.PENDING);
        Page<BookingResponse> page = new PageImpl<>(List.of(response), PageRequest.of(0, 10), 1);

        when(bookingService.getLandlordBookings(any(), any())).thenReturn(page);

        mockMvc.perform(get("/bookings/landlord"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1));
    }

    @Test
    @WithMockUser(roles = "LANDLORD")
    void confirmBookingShouldReturn200() throws Exception {
        when(bookingService.confirmBooking(1L)).thenReturn(buildResponse(1L, BookingStatus.CONFIRMED));

        mockMvc.perform(put("/bookings/1/confirm"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));
    }

    @Test
    @WithMockUser(roles = "LANDLORD")
    void confirmBookingShouldReturn403WhenNotOwner() throws Exception {
        when(bookingService.confirmBooking(1L))
                .thenThrow(new ForbiddenException("Pas votre annonce"));

        mockMvc.perform(put("/bookings/1/confirm"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "LANDLORD")
    void rejectBookingShouldReturn200() throws Exception {
        when(bookingService.rejectBooking(eq(1L), any()))
                .thenReturn(buildResponse(1L, BookingStatus.CANCELLED));

        BookingStatusUpdateRequest body = new BookingStatusUpdateRequest();
        body.setReason("Logement indisponible");

        mockMvc.perform(put("/bookings/1/reject")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    @WithMockUser
    void cancelBookingShouldReturn200() throws Exception {
        when(bookingService.cancelBooking(1L)).thenReturn(buildResponse(1L, BookingStatus.CANCELLED));

        mockMvc.perform(put("/bookings/1/cancel"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    @WithMockUser
    void cancelBookingShouldReturn400WhenAlreadyCompleted() throws Exception {
        when(bookingService.cancelBooking(1L))
                .thenThrow(new BusinessException("Impossible d'annuler"));

        mockMvc.perform(put("/bookings/1/cancel"))
                .andExpect(status().isBadRequest());
    }
}
