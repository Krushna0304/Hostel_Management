package com.krunity.HostelManagment.security;

import com.krunity.HostelManagment.security.JwtUtils;
import com.krunity.HostelManagment.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Slf4j
public class JwtFilter extends OncePerRequestFilter {
    private final JwtUtils jwtUtil;
    private final UserRepository userRepository;
    public JwtFilter(JwtUtils jwtUtil, UserRepository userRepository) {
        this.jwtUtil = jwtUtil; this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws IOException, ServletException {

        String authHeader = req.getHeader("Authorization");
        String requestURI = req.getRequestURI();
        
        log.debug("Request URI: {}, Auth header present: {}", requestURI, authHeader != null);

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            log.debug("Token extracted, length: {}", token.length());

            if (jwtUtil.validateToken(token)) {
                String username = jwtUtil.getSubjectFromToken(token);
                log.debug("Token valid for user: {}", username);
                var userOpt = userRepository.findByUsername(username);

                if (userOpt.isPresent()) {
                    var user = userOpt.get();
                    log.debug("User authenticated: {}, role: {}", user.getUsername(), user.getRole());

                    // Create authentication object (no authorities)
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(user, null, Collections.emptyList());

                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(req));

                    // Set the authentication in SecurityContext
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    log.debug("Authentication set successfully for user: {}", username);
                } else {
                    log.warn("User not found for username: {}", username);
                }
            } else {
                log.warn("JWT token validation failed for URI: {}", requestURI);
            }
        } else {
            log.trace("No Authorization header for URI: {}", requestURI);
        }

        // Continue with the filter chain
        chain.doFilter(req, res);
    }

}
