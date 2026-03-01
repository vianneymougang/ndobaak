package com.ndobaak.backend.admin.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ndobaak.backend.admin.dto.RejectListingRequest;
import com.ndobaak.backend.admin.service.AdminListingService;
import com.ndobaak.backend.auth.service.CustomOAuth2UserService;
import com.ndobaak.backend.common.enums.ListingStatus;
import com.ndobaak.backend.common.enums.RoomType;
import com.ndobaak.backend.common.exception.BusinessException;
import com.ndobaak.backend.common.exception.ResourceNotFoundException;
import com.ndobaak.backend.common.security.JwtAuthenticationFilter;
import com.ndobaak.backend.common.security.OAuth2AuthenticationFailureHandler;
import com.ndobaak.backend.common.security.OAuth2AuthenticationSuccessHandler;
import com.ndobaak.backend.listing.dto.ListingResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminController.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AdminListingService adminListingService;
    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;
    @MockitoBean
    private CustomOAuth2UserService customOAuth2UserService;
    @MockitoBean
    private OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler;
    @MockitoBean
    private OAuth2AuthenticationFailureHandler oAuth2AuthenticationFailureHandler;

    private ListingResponse buildPendingListing(Long id) {
        return ListingResponse.builder()
                .id(id)
                .title("Chambre campus test")
                .roomType(RoomType.SINGLE_ROOM)
                .monthlyPrice(new BigDecimal("30000"))
                .status(ListingStatus.PENDING_REVIEW)
                .photos(Collections.emptyList())
                .amenities(Collections.emptyList())
                .build();
    }

    @Test
    void getPendingListingsShouldReturn200() throws Exception {
        when(adminListingService.getPendingListings()).thenReturn(List.of(buildPendingListing(1L)));

        mockMvc.perform(get("/admin/listings/pending"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].status").value("PENDING_REVIEW"));
    }

    @Test
    void getPendingListingsShouldReturnEmptyListWhenNone() throws Exception {
        when(adminListingService.getPendingListings()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/admin/listings/pending"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void validateListingShouldReturn200() throws Exception {
        ListingResponse published = ListingResponse.builder()
                .id(1L)
                .title("Chambre campus test")
                .status(ListingStatus.PUBLISHED)
                .photos(Collections.emptyList())
                .amenities(Collections.emptyList())
                .build();

        when(adminListingService.validateListing(1L)).thenReturn(published);

        mockMvc.perform(put("/admin/listings/1/validate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PUBLISHED"));
    }

    @Test
    void validateListingShouldReturn404WhenNotFound() throws Exception {
        when(adminListingService.validateListing(99L))
                .thenThrow(new ResourceNotFoundException("Listing", "id", 99L));

        mockMvc.perform(put("/admin/listings/99/validate"))
                .andExpect(status().isNotFound());
    }

    @Test
    void validateListingShouldReturn400WhenNotPending() throws Exception {
        when(adminListingService.validateListing(1L))
                .thenThrow(new BusinessException("Listing must be in PENDING_REVIEW status"));

        mockMvc.perform(put("/admin/listings/1/validate"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectListingShouldReturn200WithReason() throws Exception {
        RejectListingRequest request = new RejectListingRequest();
        request.setReason("Photos de mauvaise qualité");

        ListingResponse rejected = ListingResponse.builder()
                .id(1L)
                .title("Chambre campus test")
                .status(ListingStatus.REJECTED)
                .rejectionReason("Photos de mauvaise qualité")
                .photos(Collections.emptyList())
                .amenities(Collections.emptyList())
                .build();

        when(adminListingService.rejectListing(eq(1L), eq("Photos de mauvaise qualité"))).thenReturn(rejected);

        mockMvc.perform(put("/admin/listings/1/reject")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"))
                .andExpect(jsonPath("$.rejectionReason").value("Photos de mauvaise qualité"));
    }

    @Test
    void rejectListingShouldReturn400WhenReasonMissing() throws Exception {
        RejectListingRequest request = new RejectListingRequest();

        mockMvc.perform(put("/admin/listings/1/reject")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
