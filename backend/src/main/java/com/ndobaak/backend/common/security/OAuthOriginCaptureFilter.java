package com.ndobaak.backend.common.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;

public class OAuthOriginCaptureFilter extends OncePerRequestFilter {

    private static final Set<String> ALLOWED_ORIGINS = Set.of("login", "register");

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                     FilterChain chain) throws IOException, ServletException {
        if (request.getRequestURI().contains("/oauth2/authorization/")) {
            String from = request.getParameter("from");
            if (from != null && ALLOWED_ORIGINS.contains(from)) {
                request.getSession(true).setAttribute("oauth_from", from);
            }
        }
        chain.doFilter(request, response);
    }
}
