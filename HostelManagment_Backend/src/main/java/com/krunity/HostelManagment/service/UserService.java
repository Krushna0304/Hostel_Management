package com.krunity.HostelManagment.service;

import com.krunity.HostelManagment.Mapper.UserMapper;
import com.krunity.HostelManagment.dto.CreateUserRequest;
import com.krunity.HostelManagment.dto.LoginRequest;
import com.krunity.HostelManagment.dto.UserResponse;
import com.krunity.HostelManagment.exception.AlreadyExistException;
import com.krunity.HostelManagment.exception.NotFoundException;
import com.krunity.HostelManagment.exception.UnauthorizedException;
import com.krunity.HostelManagment.model.Role;
import com.krunity.HostelManagment.model.User;
import com.krunity.HostelManagment.repository.RoleRepository;
import com.krunity.HostelManagment.repository.UserRepository;
import com.krunity.HostelManagment.security.JwtUtils;
import org.springframework.stereotype.Service;

import java.io.NotActiveException;

@Service
public class UserService {

    private final JwtUtils jwtUtils;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    public UserService(UserRepository userRepository, RoleRepository roleRepository,JwtUtils jwtUtils) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.jwtUtils = jwtUtils;
    }

    public void createUser(CreateUserRequest createUserRequest) {

        if(userRepository.existsByUsername(createUserRequest.getUsername()))
            throw new AlreadyExistException("Username already exists");


        if(userRepository.existsByPhoneNumber(createUserRequest.getPhoneNumber()))
            throw new AlreadyExistException("PhoneNumber already exists");

        Role role = roleRepository.findByName(createUserRequest.getRole())
                .orElseThrow(() -> new NotFoundException("Role not found"));

        User user = UserMapper.toEntity(createUserRequest, role);
        userRepository.save(user);
    }

    public UserResponse getUser(String username) {
        User user = userRepository.findByUsername(username).orElseThrow(
                ()-> new NotFoundException("User not found with username: " + username)
        );
        return UserMapper.toResponse(user);
    }

    public UserResponse getUser(String username,String roleName) {
        Role role = roleRepository.findByName(roleName).orElseThrow(
                ()-> new NotFoundException("Role not found")
        );
        User user = userRepository.findByUsernameAndRole(username,role).orElseThrow(
                ()-> new NotFoundException("User not found")
        );
        return UserMapper.toResponse(user);
    }

    public UserResponse updateUser(String username,CreateUserRequest createUserRequest) {
        User  user = userRepository.findByUsername(username).orElseThrow(
                () -> new NotFoundException()
        );
        Role role = roleRepository.findByName(createUserRequest.getRole()).orElseThrow(
                ()-> new NotFoundException("Role not found with role Name: " + createUserRequest.getRole())
        );

        user.setUsername(createUserRequest.getUsername());
        user.setActive(createUserRequest.getIsActive());
        user.setDisplayName(createUserRequest.getDisplayName());
        user.setRole(role);
        user.setPasswordHash(createUserRequest.getPassword());
        userRepository.save(user);
        return  UserMapper.toResponse(user);
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

        if(!user.getPasswordHash().equals(loginRequest.getPassword()) || !user.getRole().getName().equals(loginRequest.getRole())){
            throw new NotFoundException("Invalid Credentials");
        }

        if(!user.isActive()){
            throw new UnauthorizedException("Not Active User");
        }
        // In real application, generate JWT or session token here
        return jwtUtils.generateToken(loginRequest.getUsername());
    }
}
