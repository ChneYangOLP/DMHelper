package com.DMHelper.api;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

final class DesktopClientFilter extends OncePerRequestFilter {
    static final String TOKEN_HEADER = "X-DMHelper-Token";
    private final String clientToken;

    DesktopClientFilter(String clientToken) {
        this.clientToken = clientToken;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String origin = request.getHeader("Origin");
        if (!isAllowedOrigin(origin)) {
            reject(response, HttpServletResponse.SC_FORBIDDEN, "Origin not allowed");
            return;
        }

        if (origin != null) {
            response.setHeader("Access-Control-Allow-Origin", origin);
            response.addHeader("Vary", "Origin");
        }
        response.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        response.setHeader("Access-Control-Allow-Headers", "Content-Type, " + TOKEN_HEADER);
        response.setHeader("Access-Control-Max-Age", "600");

        if ("OPTIONS".equals(request.getMethod())) {
            response.setStatus(HttpServletResponse.SC_NO_CONTENT);
        } else if (!clientToken.equals(request.getHeader(TOKEN_HEADER))) {
            reject(response, HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized client");
        } else {
            chain.doFilter(request, response);
        }
    }

    private static boolean isAllowedOrigin(String origin) {
        return origin == null || "null".equals(origin)
                || "http://localhost:5173".equals(origin)
                || "http://127.0.0.1:5173".equals(origin);
    }

    private static void reject(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("text/plain;charset=UTF-8");
        response.getWriter().write(message);
    }
}
