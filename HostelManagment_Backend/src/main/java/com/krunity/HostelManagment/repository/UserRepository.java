package com.krunity.HostelManagment.repository;

import com.krunity.HostelManagment.model.Role;
import com.krunity.HostelManagment.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByUsername(String username);
    boolean existsByUsername(String username);
    Optional<User> findByUsernameAndRole(String username, Role role);
    boolean existsByPhoneNumber(String phoneNumber);
}
