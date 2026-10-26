package com.example.appeveapigateway;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

@Component
public class JwtForwardingFilter extends OncePerRequestFilter implements Ordered {

    private static final String TOKEN_COOKIE = "token";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // 1. Si la petición ya trae el header Authorization, la dejamos pasar intacta
        if (StringUtils.hasText(request.getHeader(HttpHeaders.AUTHORIZATION))) {
            filterChain.doFilter(request, response);
            return;
        }

        // 2. Buscamos la cookie llamada "token"
        String token = null;
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if (TOKEN_COOKIE.equals(cookie.getName())) {
                    token = cookie.getValue();
                    break;
                }
            }
        }

        // 3. Si no hay cookie de token, seguimos el flujo normal sin modificar nada
        if (!StringUtils.hasText(token)) {
            filterChain.doFilter(request, response);
            return;
        }

        // 4. Si hay token en la cookie, envolvemos la petición para inyectar el Header
        final String finalToken = token;
        HttpServletRequestWrapper wrappedRequest = new HttpServletRequestWrapper(request) {
            @Override
            public String getHeader(String name) {
                if (HttpHeaders.AUTHORIZATION.equalsIgnoreCase(name)) {
                    return "Bearer " + finalToken;
                }
                return super.getHeader(name);
            }

            @Override
            public Enumeration<String> getHeaders(String name) {
                if (HttpHeaders.AUTHORIZATION.equalsIgnoreCase(name)) {
                    return Collections.enumeration(Collections.singletonList("Bearer " + finalToken));
                }
                return super.getHeaders(name);
            }

            @Override
            public Enumeration<String> getHeaderNames() {
                List<String> names = Collections.list(super.getHeaderNames());
                // Evitar duplicados si por alguna razón extraña ya existía
                if (!names.contains(HttpHeaders.AUTHORIZATION) && !names.contains(HttpHeaders.AUTHORIZATION.toLowerCase())) {
                    names.add(HttpHeaders.AUTHORIZATION);
                }
                return Collections.enumeration(names);
            }
        };

        // 5. Continuamos la cadena de filtros pero enviando nuestra petición modificada
        filterChain.doFilter(wrappedRequest, response);
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }
}