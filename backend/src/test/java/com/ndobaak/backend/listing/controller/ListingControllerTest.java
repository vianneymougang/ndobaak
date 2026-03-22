package com.ndobaak.backend.listing.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ndobaak.backend.auth.service.CustomOAuth2UserService;
import com.ndobaak.backend.common.enums.ListingStatus;
import com.ndobaak.backend.common.enums.RoomType;
import com.ndobaak.backend.common.exception.BusinessException;
import com.ndobaak.backend.common.exception.ForbiddenException;
import com.ndobaak.backend.common.exception.ResourceNotFoundException;
import com.ndobaak.backend.common.security.JwtAuthenticationFilter;
import com.ndobaak.backend.common.security.OAuth2AuthenticationFailureHandler;
import com.ndobaak.backend.common.security.OAuth2AuthenticationSuccessHandler;
import com.ndobaak.backend.listing.dto.*;
import com.ndobaak.backend.auth.service.AuthService;
import com.ndobaak.backend.listing.service.ListingSearchService;
import com.ndobaak.backend.listing.service.ListingService;
import com.ndobaak.backend.listing.service.ListingViewService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ListingController.class)
@AutoConfigureMockMvc(addFilters = false)
class ListingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ListingService listingService;
    @MockitoBean
    private ListingSearchService listingSearchService;
    @MockitoBean
    private ListingViewService listingViewService;
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

    private ListingResponse buildListingResponse(Long id) {
        return ListingResponse.builder()
                .id(id)
                .title("Chambre meublée à Melen")
                .description("Belle chambre proche du campus")
                .roomType(RoomType.SINGLE_ROOM)
                .monthlyPrice(new BigDecimal("35000"))
                .availableRooms(2)
                .status(ListingStatus.DRAFT)
                .photos(Collections.emptyList())
                .amenities(Collections.emptyList())
                .build();
    }

    @Test
    void searchShouldReturn200WithoutFilters() throws Exception {
        SearchPageResponse<ListingSearchResponse> page = SearchPageResponse.<ListingSearchResponse>builder()
                .content(Collections.emptyList())
                .totalElements(0)
                .totalPages(0)
                .page(0)
                .size(10)
                .build();

        when(listingSearchService.search(any())).thenReturn(page);

        mockMvc.perform(get("/listings/search"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0))
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    void searchShouldReturn200WithKeywordFilter() throws Exception {
        ListingSearchResponse item = ListingSearchResponse.builder()
                .id(1L)
                .title("Studio Melen")
                .monthlyPrice(new BigDecimal("40000"))
                .roomType(RoomType.STUDIO)
                .availableRooms(1)
                .build();

        SearchPageResponse<ListingSearchResponse> page = SearchPageResponse.<ListingSearchResponse>builder()
                .content(List.of(item))
                .totalElements(1)
                .totalPages(1)
                .page(0)
                .size(10)
                .build();

        when(listingSearchService.search(any())).thenReturn(page);

        mockMvc.perform(get("/listings/search").param("keyword", "studio"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].title").value("Studio Melen"));
    }

    @Test
    void getByIdShouldReturn200() throws Exception {
        when(listingService.getListingById(1L)).thenReturn(buildListingResponse(1L));

        mockMvc.perform(get("/listings/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("Chambre meublée à Melen"));
    }

    @Test
    void getByIdShouldReturn404WhenNotFound() throws Exception {
        when(listingService.getListingById(99L)).thenThrow(new ResourceNotFoundException("Listing", "id", 99L));

        mockMvc.perform(get("/listings/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "LANDLORD")
    void createListingShouldReturn201() throws Exception {
        CreateListingRequest request = new CreateListingRequest();
        request.setTitle("Chambre meublée à Melen près du campus");
        request.setDescription("Belle chambre meublée, spacieuse et bien éclairée, située à proximité du campus universitaire");
        request.setRoomType(RoomType.SINGLE_ROOM);
        request.setMonthlyPrice(new BigDecimal("35000"));

        when(listingService.createListing(any())).thenReturn(buildListingResponse(1L));

        mockMvc.perform(post("/listings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    @WithMockUser(roles = "LANDLORD")
    void submitForReviewShouldReturn200() throws Exception {
        ListingResponse submitted = ListingResponse.builder()
                .id(1L)
                .title("Chambre meublée à Melen")
                .status(ListingStatus.PENDING_REVIEW)
                .photos(Collections.emptyList())
                .amenities(Collections.emptyList())
                .build();

        when(listingService.submitForReview(1L)).thenReturn(submitted);

        mockMvc.perform(put("/listings/1/submit"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING_REVIEW"));
    }

    @Test
    @WithMockUser(roles = "LANDLORD")
    void submitForReviewShouldReturn403WhenNotOwner() throws Exception {
        when(listingService.submitForReview(1L)).thenThrow(new ForbiddenException("Not the owner of this listing"));

        mockMvc.perform(put("/listings/1/submit"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "LANDLORD")
    void submitForReviewShouldReturn400WhenInvalidListing() throws Exception {
        when(listingService.submitForReview(1L)).thenThrow(new BusinessException("Title too short"));

        mockMvc.perform(put("/listings/1/submit"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "LANDLORD")
    void getMyListingsShouldReturn200() throws Exception {
        when(listingService.getMyListings()).thenReturn(List.of(buildListingResponse(1L)));

        mockMvc.perform(get("/listings/my"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").value(1));
    }
}
