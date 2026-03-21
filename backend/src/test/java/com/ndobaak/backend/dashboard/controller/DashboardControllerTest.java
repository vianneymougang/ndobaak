package com.ndobaak.backend.dashboard.controller;

import com.ndobaak.backend.auth.service.CustomOAuth2UserService;
import com.ndobaak.backend.common.security.JwtAuthenticationFilter;
import com.ndobaak.backend.common.security.OAuth2AuthenticationFailureHandler;
import com.ndobaak.backend.common.security.OAuth2AuthenticationSuccessHandler;
import com.ndobaak.backend.dashboard.dto.LandlordDashboardResponse;
import com.ndobaak.backend.dashboard.service.DashboardService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DashboardController.class)
@AutoConfigureMockMvc(addFilters = false)
class DashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DashboardService dashboardService;
    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;
    @MockitoBean
    private CustomOAuth2UserService customOAuth2UserService;
    @MockitoBean
    private OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler;
    @MockitoBean
    private OAuth2AuthenticationFailureHandler oAuth2AuthenticationFailureHandler;

    private LandlordDashboardResponse buildFullResponse() {
        LandlordDashboardResponse.PendingBookingSummary pendingBooking =
                LandlordDashboardResponse.PendingBookingSummary.builder()
                        .bookingId(1L)
                        .studentName("John Doe")
                        .listingTitle("Studio Eyang")
                        .startDate("2026-04-01")
                        .createdAt("2026-03-18T10:00:00")
                        .build();

        LandlordDashboardResponse.RecentActivityItem activity =
                LandlordDashboardResponse.RecentActivityItem.builder()
                        .type("BOOKING_CREATED")
                        .title("Nouvelle reservation")
                        .message("Une reservation a ete creee.")
                        .createdAt("2026-03-18T10:00:00")
                        .referenceId(1L)
                        .build();

        return LandlordDashboardResponse.builder()
                .totalListings(5)
                .activeListings(3)
                .pendingBookings(2)
                .totalRevenue(new BigDecimal("210000"))
                .recentPendingBookings(List.of(pendingBooking))
                .recentActivities(List.of(activity))
                .build();
    }

    @Test
    @WithMockUser(roles = "LANDLORD")
    void getLandlordDashboardShouldReturn200() throws Exception {
        when(dashboardService.getLandlordDashboard(any(), any())).thenReturn(buildFullResponse());

        mockMvc.perform(get("/dashboard/landlord"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalListings").value(5))
                .andExpect(jsonPath("$.activeListings").value(3))
                .andExpect(jsonPath("$.pendingBookings").value(2))
                .andExpect(jsonPath("$.totalRevenue").value(210000))
                .andExpect(jsonPath("$.recentPendingBookings").isArray())
                .andExpect(jsonPath("$.recentActivities").isArray());
    }

    @Test
    @WithMockUser(roles = "LANDLORD")
    void getLandlordDashboardShouldReturnCorrectJsonStructure() throws Exception {
        when(dashboardService.getLandlordDashboard(any(), any())).thenReturn(buildFullResponse());

        mockMvc.perform(get("/dashboard/landlord"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recentPendingBookings[0].bookingId").value(1))
                .andExpect(jsonPath("$.recentPendingBookings[0].studentName").value("John Doe"))
                .andExpect(jsonPath("$.recentPendingBookings[0].listingTitle").value("Studio Eyang"))
                .andExpect(jsonPath("$.recentPendingBookings[0].startDate").value("2026-04-01"))
                .andExpect(jsonPath("$.recentPendingBookings[0].createdAt").value("2026-03-18T10:00:00"))
                .andExpect(jsonPath("$.recentActivities[0].type").value("BOOKING_CREATED"))
                .andExpect(jsonPath("$.recentActivities[0].title").value("Nouvelle reservation"))
                .andExpect(jsonPath("$.recentActivities[0].message").value("Une reservation a ete creee."))
                .andExpect(jsonPath("$.recentActivities[0].createdAt").value("2026-03-18T10:00:00"))
                .andExpect(jsonPath("$.recentActivities[0].referenceId").value(1));
    }

    @Test
    @WithMockUser(roles = "LANDLORD")
    void getLandlordDashboardShouldOmitNullRevenue() throws Exception {
        LandlordDashboardResponse response = LandlordDashboardResponse.builder()
                .totalListings(0)
                .activeListings(0)
                .pendingBookings(0)
                .totalRevenue(null)
                .recentPendingBookings(List.of())
                .recentActivities(List.of())
                .build();

        when(dashboardService.getLandlordDashboard(any(), any())).thenReturn(response);

        mockMvc.perform(get("/dashboard/landlord"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalRevenue").doesNotExist())
                .andExpect(jsonPath("$.totalListings").value(0))
                .andExpect(jsonPath("$.recentPendingBookings").isEmpty())
                .andExpect(jsonPath("$.recentActivities").isEmpty());
    }
}
