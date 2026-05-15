package com.krunity.HostelManagment.controller;

import com.krunity.HostelManagment.dto.CreateUserRequest;
import com.krunity.HostelManagment.dto.LoginRequest;
import com.krunity.HostelManagment.dto.UpdateProfileRequest;
import com.krunity.HostelManagment.dto.UserResponse;
import com.krunity.HostelManagment.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/users")
public class UserController {

    //CURD operations for User

    private final UserService userService;
    public UserController(UserService userService) {
        this.userService = userService;
    }

    //create User
    @PostMapping
    public ResponseEntity<?> createUser(@RequestBody @Valid CreateUserRequest createUserRequest) {
            userService.createUser(createUserRequest);
            return ResponseEntity.status(201).build();
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody @Valid LoginRequest loginRequest) {
            String token = userService.loginUser(loginRequest);
            return ResponseEntity.status(200).body(Map.of("token", token));
    }

    //Get User
    @GetMapping("/{username}")
    public ResponseEntity<UserResponse> getUser(@PathVariable String username) {
        try{
            UserResponse userResponse = userService.getUser(username);
            return ResponseEntity.status(200).body(userResponse);
        }catch(Exception ex){
            return ResponseEntity.status(500).build();
        }
    }


    //Get user by username and role
    @GetMapping("/{username}/role/{roleName}")
    public ResponseEntity<UserResponse> getUserBy(@PathVariable String username,@PathVariable String roleName) {
            UserResponse userResponse = userService.getUser(username,roleName);
            return ResponseEntity.status(200).body(userResponse);
    }

    //update User
    @PutMapping("/{username}")
    public ResponseEntity<UserResponse> updateUser(@PathVariable String username,@RequestBody @Valid CreateUserRequest createUserRequest) {
        try{
            UserResponse userResponse = userService.updateUser(username,createUserRequest);
            return ResponseEntity.status(200).build();
        }
        catch(Exception ex){
            return ResponseEntity.status(500).build();
        }
    }

    @DeleteMapping("/{username}")
    public ResponseEntity<UserResponse> deleteUser(@PathVariable String username) {
        try{
            userService.deleteUser(username);
            return ResponseEntity.noContent().build();
        }catch(Exception ex){
            return ResponseEntity.status(500).build();
        }
    }

    // Update profile (displayName, phoneNumber, optional password)
    @PatchMapping("/{username}/profile")
    public ResponseEntity<UserResponse> updateProfile(
            @PathVariable String username,
            @RequestBody @Valid UpdateProfileRequest request) {
        UserResponse userResponse = userService.updateProfile(username, request);
        return ResponseEntity.ok(userResponse);
    }
}
