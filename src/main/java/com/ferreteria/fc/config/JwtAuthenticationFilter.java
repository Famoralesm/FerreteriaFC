
package com.ferreteria.fc.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log =
            LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;

    public JwtAuthenticationFilter(
            JwtService jwtService,
            CustomUserDetailsService userDetailsService) {

        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        final String authHeader =
                request.getHeader("Authorization");

        String uri = request.getRequestURI();

        if (authHeader == null || authHeader.isBlank()) {
            log.warn("JWT AUSENTE | URI: {}", uri);
            filterChain.doFilter(request, response);
            return;
        }

        if (!authHeader.startsWith("Bearer ")) {
            log.warn("FORMATO AUTHORIZATION INVALIDO | URI: {}", uri);
            filterChain.doFilter(request, response);
            return;
        }

        String jwt = authHeader.substring(7);

        try {

            String email = jwtService.extractUsername(jwt);

            if (email == null || email.isBlank()) {
                log.warn("JWT SIN SUBJECT | URI: {}", uri);
                filterChain.doFilter(request, response);
                return;
            }

            if (SecurityContextHolder.getContext()
                    .getAuthentication() == null) {

                UserDetails userDetails =
                        userDetailsService.loadUserByUsername(email);

                if (!jwtService.isTokenValid(jwt, userDetails)) {

                    log.warn(
                            "JWT NO VALIDO | URI: {} | EMAIL: {}",
                            uri,
                            email
                    );

                    SecurityContextHolder.clearContext();

                    response.setStatus(
                            HttpServletResponse.SC_UNAUTHORIZED
                    );

                    response.setContentType("application/json");
                    response.setCharacterEncoding("UTF-8");

                    response.getWriter().write(
                            "{\"code\":\"TOKEN_INVALID_OR_EXPIRED\",\"message\":\"La sesión no es válida o ha expirado\"}"
                    );

                    return;
                }

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                userDetails,
                                null,
                                userDetails.getAuthorities()
                        );

                authentication.setDetails(
                        new WebAuthenticationDetailsSource()
                                .buildDetails(request)
                );

                SecurityContextHolder.getContext()
                        .setAuthentication(authentication);

                log.info(
                        "JWT VALIDADO | URI: {} | EMAIL: {} | AUTHORITIES: {}",
                        uri,
                        email,
                        userDetails.getAuthorities()
                );
            }

        } catch (
                io.jsonwebtoken.JwtException |
                IllegalArgumentException |
                UsernameNotFoundException ex
        ) {

            log.error(
                    "ERROR VALIDANDO JWT | URI: {} | TIPO: {} | MENSAJE: {}",
                    uri,
                    ex.getClass().getSimpleName(),
                    ex.getMessage()
            );

            SecurityContextHolder.clearContext();

            response.setStatus(
                    HttpServletResponse.SC_UNAUTHORIZED
            );

            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");

            response.getWriter().write(
                    "{\"code\":\"TOKEN_INVALID_OR_EXPIRED\",\"message\":\"La sesión no es válida o ha expirado\"}"
            );

            return;
        }

        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return "/api/auth/login".equals(request.getServletPath());
    }
}
