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

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);

            if (jwtUtil.validateToken(token)) {
                String username = jwtUtil.getSubjectFromToken(token);
                var userOpt = userRepository.findByUsername(username);

                if (userOpt.isPresent()) {
                    var user = userOpt.get();

                    // Create authentication object (no authorities)
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(user, null, Collections.emptyList());

                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(req));

                    // ✅ Set the authentication in SecurityContext
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            }
        }

        // Continue with the filter chain
        chain.doFilter(req, res);
    }

}

