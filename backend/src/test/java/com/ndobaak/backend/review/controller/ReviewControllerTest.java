package com.ndobaak.backend.review.controller;

import com.ndobaak.backend.auth.service.CustomOAuth2UserService;
import com.ndobaak.backend.common.enums.ReviewStatus;
import com.ndobaak.backend.common.exception.BusinessException;
import com.ndobaak.backend.common.exception.ForbiddenException;
import com.ndobaak.backend.common.exception.ResourceNotFoundException;
import com.ndobaak.backend.common.security.JwtAuthenticationFilter;
import com.ndobaak.backend.common.security.OAuth2AuthenticationFailureHandler;
import com.ndobaak.backend.common.security.OAuth2AuthenticationSuccessHandler;
import com.ndobaak.backend.review.dto.RatingStatsResponse;
import com.ndobaak.backend.review.dto.ReviewResponse;
import com.ndobaak.backend.review.service.ReviewService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ReviewController.class)
@AutoConfigureMockMvc(addFilters = false)
class ReviewControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ReviewService reviewService;
    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;
    @MockitoBean
    private CustomOAuth2UserService customOAuth2UserService;
    @MockitoBean
    private OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler;
    @MockitoBean
    private OAuth2AuthenticationFailureHandler oAuth2AuthenticationFailureHandler;

    private ReviewResponse buildReviewResponse(Long id) {
        return ReviewResponse.builder()
                .id(id)
                .studentName("Alice Martin")
                .rating(4)
                .comment("Très bon logement")
                .status(ReviewStatus.APPROVED)
                .createdAt(LocalDateTime.of(2026, 3, 18, 10, 0))
                .build();
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    void createReviewShouldReturn201() throws Exception {
        String reviewJson = "{\"listingId\":100,\"rating\":4,\"comment\":\"Très bon logement, je recommande vivement\"}";

        MockMultipartFile reviewPart = new MockMultipartFile(
                "review", "", MediaType.APPLICATION_JSON_VALUE, reviewJson.getBytes());

        when(reviewService.createReview(any(), any())).thenReturn(buildReviewResponse(1L));

        mockMvc.perform(multipart("/reviews")
                        .file(reviewPart))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.rating").value(4));
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    void createReviewWithPhotosShouldReturn201() throws Exception {
        String reviewJson = "{\"listingId\":100,\"rating\":5,\"comment\":\"Excellent séjour dans ce logement\"}";

        MockMultipartFile reviewPart = new MockMultipartFile(
                "review", "", MediaType.APPLICATION_JSON_VALUE, reviewJson.getBytes());
        MockMultipartFile photo = new MockMultipartFile(
                "photos", "photo.jpg", MediaType.IMAGE_JPEG_VALUE, new byte[]{1, 2, 3});

        when(reviewService.createReview(any(), any())).thenReturn(buildReviewResponse(1L));

        mockMvc.perform(multipart("/reviews")
                        .file(reviewPart)
                        .file(photo))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    void createReviewShouldReturn400OnBusinessError() throws Exception {
        String reviewJson = "{\"listingId\":100,\"rating\":4,\"comment\":\"Bon logement pour les étudiants\"}";

        MockMultipartFile reviewPart = new MockMultipartFile(
                "review", "", MediaType.APPLICATION_JSON_VALUE, reviewJson.getBytes());

        when(reviewService.createReview(any(), any()))
                .thenThrow(new BusinessException("Pas de réservation éligible"));

        mockMvc.perform(multipart("/reviews")
                        .file(reviewPart))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getReviewsByListingShouldReturn200() throws Exception {
        ReviewResponse response = buildReviewResponse(1L);
        Page<ReviewResponse> page = new PageImpl<>(List.of(response), PageRequest.of(0, 20), 1);

        when(reviewService.getReviewsByListing(any(), any())).thenReturn(page);

        mockMvc.perform(get("/listings/100/reviews"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].studentName").value("Alice Martin"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void getReviewsByListingShouldReturn404WhenListingNotFound() throws Exception {
        when(reviewService.getReviewsByListing(any(), any()))
                .thenThrow(new ResourceNotFoundException("Listing", "id", 999L));

        mockMvc.perform(get("/listings/999/reviews"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    void getMyReviewsShouldReturn200() throws Exception {
        ReviewResponse response = buildReviewResponse(1L);
        Page<ReviewResponse> page = new PageImpl<>(List.of(response), PageRequest.of(0, 20), 1);

        when(reviewService.getMyReviews(any())).thenReturn(page);

        mockMvc.perform(get("/reviews/my"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].rating").value(4));
    }

    @Test
    @WithMockUser
    void deleteReviewShouldReturn204() throws Exception {
        doNothing().when(reviewService).deleteReview(1L);

        mockMvc.perform(delete("/reviews/1"))
                .andExpect(status().isNoContent());

        verify(reviewService).deleteReview(1L);
    }

    @Test
    @WithMockUser
    void deleteReviewShouldReturn403WhenNotOwner() throws Exception {
        doThrow(new ForbiddenException("Pas votre avis"))
                .when(reviewService).deleteReview(1L);

        mockMvc.perform(delete("/reviews/1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser
    void deleteReviewShouldReturn404WhenNotFound() throws Exception {
        doThrow(new ResourceNotFoundException("Review", "id", 999L))
                .when(reviewService).deleteReview(999L);

        mockMvc.perform(delete("/reviews/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getRatingStatsShouldReturn200() throws Exception {
        RatingStatsResponse stats = RatingStatsResponse.builder()
                .averageRating(4.3)
                .totalReviews(15L)
                .distribution(Map.of(5, 8L, 4, 4L, 3, 2L, 2, 1L, 1, 0L))
                .build();

        when(reviewService.getRatingStats(100L)).thenReturn(stats);

        mockMvc.perform(get("/listings/100/rating-stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.averageRating").value(4.3))
                .andExpect(jsonPath("$.totalReviews").value(15))
                .andExpect(jsonPath("$.distribution.5").value(8));
    }

    @Test
    void getRatingStatsShouldReturn404WhenListingNotFound() throws Exception {
        when(reviewService.getRatingStats(999L))
                .thenThrow(new ResourceNotFoundException("Listing", "id", 999L));

        mockMvc.perform(get("/listings/999/rating-stats"))
                .andExpect(status().isNotFound());
    }
}
