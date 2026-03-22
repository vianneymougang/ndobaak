package com.ndobaak.backend.common.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

class RateLimitFilterTest {

    private RateLimitFilter filter;

    @BeforeEach
    void setUp() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        filter = new RateLimitFilter(mapper);
    }

    @Test
    void shouldAllowRequestsWithinLimit() throws Exception {
        for (int i = 0; i < 5; i++) {
            MockHttpServletResponse response = doRequest("/auth/login", "192.168.1.1");
            assertThat(response.getStatus()).isEqualTo(200);
        }
    }

    @Test
    void shouldBlockRequestsExceedingLimit() throws Exception {
        for (int i = 0; i < 5; i++) {
            doRequest("/auth/login", "10.0.0.1");
        }

        MockHttpServletResponse response = doRequest("/auth/login", "10.0.0.1");
        assertThat(response.getStatus()).isEqualTo(429);
        assertThat(response.getContentAsString()).contains("Trop de tentatives");
    }

    @Test
    void shouldTrackIpsSeparately() throws Exception {
        for (int i = 0; i < 5; i++) {
            doRequest("/auth/login", "10.0.0.1");
        }

        MockHttpServletResponse blocked = doRequest("/auth/login", "10.0.0.1");
        assertThat(blocked.getStatus()).isEqualTo(429);

        MockHttpServletResponse allowed = doRequest("/auth/login", "10.0.0.2");
        assertThat(allowed.getStatus()).isEqualTo(200);
    }

    @Test
    void shouldNotRateLimitNonAuthEndpoints() throws Exception {
        for (int i = 0; i < 20; i++) {
            MockHttpServletResponse response = doRequest("/listings/search", "10.0.0.1");
            assertThat(response.getStatus()).isEqualTo(200);
        }
    }

    @Test
    void shouldEnforceLowerLimitForForgotPassword() throws Exception {
        for (int i = 0; i < 3; i++) {
            MockHttpServletResponse response = doRequest("/auth/forgot-password", "10.0.0.3");
            assertThat(response.getStatus()).isEqualTo(200);
        }

        MockHttpServletResponse blocked = doRequest("/auth/forgot-password", "10.0.0.3");
        assertThat(blocked.getStatus()).isEqualTo(429);
    }

    @Test
    void shouldAllowHigherLimitForRefresh() throws Exception {
        for (int i = 0; i < 10; i++) {
            MockHttpServletResponse response = doRequest("/auth/refresh", "10.0.0.4");
            assertThat(response.getStatus()).isEqualTo(200);
        }

        MockHttpServletResponse blocked = doRequest("/auth/refresh", "10.0.0.4");
        assertThat(blocked.getStatus()).isEqualTo(429);
    }

    @Test
    void shouldUseXForwardedForHeader() throws Exception {
        for (int i = 0; i < 5; i++) {
            doRequestWithXff("/auth/login", "127.0.0.1", "203.0.113.50");
        }

        MockHttpServletResponse blocked = doRequestWithXff("/auth/login", "127.0.0.1", "203.0.113.50");
        assertThat(blocked.getStatus()).isEqualTo(429);

        MockHttpServletResponse allowed = doRequestWithXff("/auth/login", "127.0.0.1", "203.0.113.51");
        assertThat(allowed.getStatus()).isEqualTo(200);
    }

    @Test
    void cleanupShouldRemoveStaleEntries() throws Exception {
        doRequest("/auth/login", "10.0.0.99");
        filter.cleanupStaleEntries();
    }

    private MockHttpServletResponse doRequest(String path, String remoteAddr) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", path);
        request.setServletPath(path);
        request.setRemoteAddr(remoteAddr);
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, new MockFilterChain());
        return response;
    }

    private MockHttpServletResponse doRequestWithXff(String path, String remoteAddr, String xff) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", path);
        request.setServletPath(path);
        request.setRemoteAddr(remoteAddr);
        request.addHeader("X-Forwarded-For", xff);
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, new MockFilterChain());
        return response;
    }
}
