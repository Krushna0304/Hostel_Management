package com.krunity.HostelManagment.Utils;

import com.krunity.HostelManagment.model.User;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class ApplicationContext {
    public static User getUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("No authenticated user found");
        }

        Object principal = authentication.getPrincipal();

        if (principal instanceof String && principal.equals("anonymousUser")) {
            return null;
            //throw new RuntimeException("Logged-in user not found");
        }

        if (principal instanceof User appUser) {
            return appUser;
        }

        throw new RuntimeException("Principal is not of type AppUser");
    }
}


