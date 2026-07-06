package com.krunity.HostelManagment.service;

import com.krunity.HostelManagment.Mapper.UserMapper;
import com.krunity.HostelManagment.dto.CreateUserRequest;
import com.krunity.HostelManagment.dto.LoginRequest;
import com.krunity.HostelManagment.dto.UpdateProfileRequest;
import com.krunity.HostelManagment.dto.UserResponse;
import com.krunity.HostelManagment.exception.AlreadyExistException;
import com.krunity.HostelManagment.exception.NotFoundException;
import com.krunity.HostelManagment.exception.UnauthorizedException;
import com.krunity.HostelManagment.model.Role;
import com.krunity.HostelManagment.model.User;
import com.krunity.HostelManagment.model.RoomAllotment;
import com.krunity.HostelManagment.enums.RoomAllotmentStatus;
import com.krunity.HostelManagment.repository.RoleRepository;
import com.krunity.HostelManagment.repository.UserRepository;
import com.krunity.HostelManagment.repository.RoomAllotmentRepository;
import com.krunity.HostelManagment.security.JwtUtils;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class UserService {

    private final JwtUtils jwtUtils;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final RoomAllotmentRepository roomAllotmentRepository;
    private final CashPaymentOtpService cashPaymentOtpService;

    public UserService(UserRepository userRepository, RoleRepository roleRepository,
                       JwtUtils jwtUtils, PasswordEncoder passwordEncoder,
                       RoomAllotmentRepository roomAllotmentRepository,
                       CashPaymentOtpService cashPaymentOtpService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.jwtUtils = jwtUtils;
        this.passwordEncoder = passwordEncoder;
        this.roomAllotmentRepository = roomAllotmentRepository;
        this.cashPaymentOtpService = cashPaymentOtpService;
    }

    public void createUser(CreateUserRequest createUserRequest) {

        if (userRepository.existsByUsername(createUserRequest.getUsername()))
            throw new AlreadyExistException("Username already exists");

        if (userRepository.existsByPhoneNumber(createUserRequest.getPhoneNumber()))
            throw new AlreadyExistException("Phone Number already exists");

        Role role = roleRepository.findByName(createUserRequest.getRole())
                .orElseThrow(() -> new NotFoundException("Role not found"));

        // Hash the password before saving
        String hashedPassword = passwordEncoder.encode(createUserRequest.getPassword());
        User user = UserMapper.toEntity(createUserRequest, role, hashedPassword);
        userRepository.save(user);

        // Seed cash-payment-method toggles for new owners (default disabled).
        if ("OWNER".equalsIgnoreCase(role.getName())) {
            cashPaymentOtpService.seedDefaultsForOwner(user.getUserId());
        }
    }

    public UserResponse getUser(String username) {
        User user = userRepository.findByUsername(username).orElseThrow(
                () -> new NotFoundException("User not found with username: " + username)
        );
        return UserMapper.toResponse(user);
    }

    public UserResponse getUser(String username, String roleName) {
        Role role = roleRepository.findByName(roleName).orElseThrow(
                () -> new NotFoundException("Role not found")
        );
        User user = userRepository.findByUsernameAndRole(username, role).orElseThrow(
                () -> new NotFoundException("User not found")
        );
        return UserMapper.toResponse(user);
    }

    public UserResponse updateUser(String username, CreateUserRequest createUserRequest) {
        User user = userRepository.findByUsername(username).orElseThrow(
                () -> new NotFoundException()
        );
        Role role = roleRepository.findByName(createUserRequest.getRole()).orElseThrow(
                () -> new NotFoundException("Role not found with role Name: " + createUserRequest.getRole())
        );

        user.setUsername(createUserRequest.getUsername());
        user.setActive(createUserRequest.getIsActive());
        user.setDisplayName(createUserRequest.getDisplayName());
        user.setRole(role);
        // Hash the new password before saving
        user.setPasswordHash(passwordEncoder.encode(createUserRequest.getPassword()));
        userRepository.save(user);
        return UserMapper.toResponse(user);
    }

    public void deleteUser(String username) {
        User user = userRepository.findByUsername(username).orElseThrow(
                () -> new NotFoundException("User not found with username: " + username)
        );
        userRepository.delete(user);
    }

    public String loginUser(LoginRequest loginRequest) {

        User user = userRepository.findByUsername(loginRequest.getUsername()).orElseThrow(
                () -> new NotFoundException("User not found")
        );

        // Use BCrypt matches — works for both hashed and (legacy) plain passwords
        boolean passwordMatch = passwordEncoder.matches(loginRequest.getPassword(), user.getPasswordHash());
        boolean roleMatch = user.getRole().getName().equals(loginRequest.getRole());

        if (!passwordMatch || !roleMatch) {
            throw new NotFoundException("Invalid Credentials");
        }

        if (!user.isActive()) {
            throw new UnauthorizedException("Not Active User");
        }

        return jwtUtils.generateToken(loginRequest.getUsername());
    }

    public UUID getCurrentUserId(Authentication authentication) {
        String username = authentication.getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new NotFoundException("User not found"));
        return user.getUserId();
    }

    /**
     * Get all tenants for a specific hostel
     */
    public List<UserResponse> getTenantsByHostel(UUID hostelId) {
        // Get the current owner
        UUID ownerId = com.krunity.HostelManagment.Utils.ApplicationContext.getUser().getUserId();
        
        // Get all confirmed room allotments for this owner
        List<RoomAllotment> allotments = roomAllotmentRepository
                .findByRoom_Hostel_Owner_UserIdAndRoomAllotmentStatus(ownerId, RoomAllotmentStatus.ACTIVE);
        
        // Filter by hostel and get unique tenants
        List<User> tenants = allotments.stream()
                .filter(allotment -> allotment.getRoom().getHostel().getHostelId().equals(hostelId))
                .map(RoomAllotment::getTenant)
                .distinct()
                .collect(Collectors.toList());
        
        return tenants.stream()
                .map(UserMapper::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Update user profile (displayName, phoneNumber, optionally password)
     */
    public UserResponse updateProfile(String username, UpdateProfileRequest request) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new NotFoundException("User not found with username: " + username));

        // Check if phone number is being changed and if it's already taken by another user
        if (!user.getPhoneNumber().equals(request.getPhoneNumber())) {
            if (userRepository.existsByPhoneNumber(request.getPhoneNumber())) {
                throw new AlreadyExistException("Phone number already exists");
            }
        }

        user.setDisplayName(request.getDisplayName());
        user.setPhoneNumber(request.getPhoneNumber());

        // Only update password if provided
        if (request.getNewPassword() != null && !request.getNewPassword().isBlank()) {
            user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        }

        userRepository.save(user);
        return UserMapper.toResponse(user);
    }
}
