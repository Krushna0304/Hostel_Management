package com.krunity.HostelManagment.config;

import com.krunity.HostelManagment.model.Role;
import com.krunity.HostelManagment.repository.RoleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private RoleRepository roleRepository;

    @Override
    public void run(String... args) throws Exception {
        //initializeRoles();
    }

    private void initializeRoles() {
        // Check if roles already exist
        if (roleRepository.count() == 0) {
            List<Role> defaultRoles = Arrays.asList(
                Role.builder().name("ADMIN").build(),
                Role.builder().name("OWNER").build(),
                Role.builder().name("TENANT").build()
            );
            
            roleRepository.saveAll(defaultRoles);
            System.out.println("✅ Default roles initialized: ADMIN, OWNER, TENANT");
        } else {
            System.out.println("ℹ️ Roles already exist, skipping initialization");
        }
    }
}