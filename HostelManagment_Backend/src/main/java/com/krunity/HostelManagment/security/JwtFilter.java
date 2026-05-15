package com.krunity.HostelManagment.security;

import com.krunity.HostelManagment.security.JwtUtils;
import com.krunity.HostelManagment.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

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
        
        System.out.println("JWT Filter - Request URI: " + requestURI);
        System.out.println("JWT Filter - Auth Header present: " + (authHeader != null));

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            System.out.println("JWT Filter - Token extracted, length: " + token.length());

            if (jwtUtil.validateToken(token)) {
                String username = jwtUtil.getSubjectFromToken(token);
                System.out.println("JWT Filter - Token valid for username: " + username);
                var userOpt = userRepository.findByUsername(username);

                if (userOpt.isPresent()) {
                    var user = userOpt.get();
                    System.out.println("JWT Filter - User found: " + user.getUsername() + ", Role: " + user.getRole());

                    // Create authentication object (no authorities)
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(user, null, Collections.emptyList());

                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(req));

                    // ✅ Set the authentication in SecurityContext
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    System.out.println("JWT Filter - Authentication set successfully");
                } else {
                    System.out.println("JWT Filter - User not found for username: " + username);
                }
            } else {
                System.out.println("JWT Filter - Token validation failed");
            }
        } else {
            System.out.println("JWT Filter - No valid Authorization header");
        }

        // Continue with the filter chain
        chain.doFilter(req, res);
    }

}

