package com.ndobaak.backend.geo;

import com.ndobaak.backend.auth.service.CustomOAuth2UserService;
import com.ndobaak.backend.common.enums.RoomType;
import com.ndobaak.backend.common.exception.BusinessException;
import com.ndobaak.backend.common.security.JwtAuthenticationFilter;
import com.ndobaak.backend.common.security.OAuth2AuthenticationFailureHandler;
import com.ndobaak.backend.common.security.OAuth2AuthenticationSuccessHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(GeoController.class)
@AutoConfigureMockMvc(addFilters = false)
class GeoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GeoService geoService;
    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;
    @MockitoBean
    private CustomOAuth2UserService customOAuth2UserService;
    @MockitoBean
    private OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler;
    @MockitoBean
    private OAuth2AuthenticationFailureHandler oAuth2AuthenticationFailureHandler;

    @Test
    void nearbyEndpointShouldReturnResults() throws Exception {
        NearbyListingResponse response = new NearbyListingResponse(
                1L, "Studio Eyang", "https://cdn.example.com/thumb.webp",
                new BigDecimal("35000"), RoomType.STUDIO, "Eyang",
                0.15, 3.8855, 11.3922, 4.5, 10, true
        );
        when(geoService.findNearby(eq(3.8845), eq(11.3912), eq(2.0)))
                .thenReturn(List.of(response));

        mockMvc.perform(get("/geo/nearby")
                        .param("lat", "3.8845")
                        .param("lng", "11.3912")
                        .param("radiusKm", "2.0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].title").value("Studio Eyang"))
                .andExpect(jsonPath("$[0].verified").value(true));
    }

    @Test
    void nearbyEndpointShouldUseDefaultRadius() throws Exception {
        when(geoService.findNearby(eq(3.8845), eq(11.3912), eq(2.0)))
                .thenReturn(Collections.emptyList());

        mockMvc.perform(get("/geo/nearby")
                        .param("lat", "3.8845")
                        .param("lng", "11.3912"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void nearbyEndpointShouldReturn400WhenMissingParams() throws Exception {
        mockMvc.perform(get("/geo/nearby"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void nearbyEndpointShouldReturn400OnInvalidCoordinates() throws Exception {
        when(geoService.findNearby(eq(91.0), anyDouble(), anyDouble()))
                .thenThrow(new BusinessException("La latitude doit être comprise entre -90 et 90."));

        mockMvc.perform(get("/geo/nearby")
                        .param("lat", "91.0")
                        .param("lng", "11.3912")
                        .param("radiusKm", "2.0"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void nearbyEndpointShouldReturn400OnInvalidRadius() throws Exception {
        when(geoService.findNearby(anyDouble(), anyDouble(), eq(15.0)))
                .thenThrow(new BusinessException("Le rayon doit être compris entre 0.1 et 10 km."));

        mockMvc.perform(get("/geo/nearby")
                        .param("lat", "3.8845")
                        .param("lng", "11.3912")
                        .param("radiusKm", "15.0"))
                .andExpect(status().isBadRequest());
    }
}
