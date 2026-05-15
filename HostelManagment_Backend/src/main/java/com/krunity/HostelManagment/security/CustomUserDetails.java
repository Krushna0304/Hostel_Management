package com.krunity.HostelManagment.security;


import com.krunity.HostelManagment.model.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;
import java.util.UUID;

public class CustomUserDetails implements UserDetails {
    private final User user;
    public CustomUserDetails(User user) { this.user = user; }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // Return the user's role as a GrantedAuthority
        // Spring Security's hasRole() expects authorities to have "ROLE_" prefix
        if (user.getRole() != null && user.getRole().getName() != null) {
            String roleName = user.getRole().getName();
            // Add ROLE_ prefix if not already present
            if (!roleName.startsWith("ROLE_")) {
                roleName = "ROLE_" + roleName;
            }
            return Collections.singletonList(new SimpleGrantedAuthority(roleName));
        }
        return Collections.emptyList();
    }

    @Override public String getPassword() { return user.getPasswordHash(); }
    @Override public String getUsername() { return user.getUsername(); }
    @Override public boolean isAccountNonExpired() { return true; }
    @Override public boolean isAccountNonLocked() { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled() { return true; }

    // convenient access
    public UUID getId() { return user.getUserId(); }
    public User getUser() { return user; }
}


