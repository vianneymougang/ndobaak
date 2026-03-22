package com.ndobaak.backend.common.security;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseCookie;

public final class CookieUtils {

    public static final String ACCESS_COOKIE = "ndb_access";
    public static final String REFRESH_COOKIE = "ndb_refresh";

    private CookieUtils() {}

    public static void addAccessCookie(HttpServletResponse response, String token,
                                       long maxAgeSeconds, boolean secure) {
        ResponseCookie cookie = ResponseCookie.from(ACCESS_COOKIE, token)
                .httpOnly(true)
                .secure(secure)
                .sameSite("Strict")
                .path("/api/v1")
                .maxAge(maxAgeSeconds)
                .build();
        response.addHeader("Set-Cookie", cookie.toString());
    }

    public static void addRefreshCookie(HttpServletResponse response, String token,
                                        long maxAgeSeconds, boolean secure) {
        ResponseCookie cookie = ResponseCookie.from(REFRESH_COOKIE, token)
                .httpOnly(true)
                .secure(secure)
                .sameSite("Strict")
                .path("/api/v1/auth")
                .maxAge(maxAgeSeconds)
                .build();
        response.addHeader("Set-Cookie", cookie.toString());
    }

    public static void clearAuthCookies(HttpServletResponse response, boolean secure) {
        addAccessCookie(response, "", 0, secure);
        addRefreshCookie(response, "", 0, secure);
    }
}
