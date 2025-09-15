package com.zettix.controller;

import com.zettix.dto.request.LoginRequest;
import com.zettix.dto.request.RegisterRequest;
import com.zettix.dto.response.AuthResponse;
import com.zettix.entity.User;
import com.zettix.security.JwtUtil;
import com.zettix.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "APIs for user authentication and registration")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserService userService;
    private final JwtUtil jwtUtil;

    @Operation(summary = "Register new user", description = "Create a new user account")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User registered successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input or username/email already exists")
    })
    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        try {
            if (userService.existsByUsername(request.getUsername())) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Username is already taken!"));
            }

            if (userService.existsByEmail(request.getEmail())) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Email is already in use!"));
            }

            User user = new User();
            user.setUsername(request.getUsername());
            user.setEmail(request.getEmail());
            user.setPassword(request.getPassword()); // Will be encoded in service
            user.setFullName(request.getFullName());
            user.setPhoneNumber(request.getPhoneNumber());
            user.setRole(User.Role.USER);

            User savedUser = userService.save(user);
            
            Map<String, Object> claims = new HashMap<>();
            claims.put("userId", savedUser.getId());
            claims.put("role", savedUser.getRole().name());
            
            String token = jwtUtil.generateToken(savedUser, claims);

            return ResponseEntity.ok(AuthResponse.builder()
                    .token(token)
                    .type("Bearer")
                    .user(savedUser)
                    .build());

        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Registration failed: " + e.getMessage()));
        }
    }

    @Operation(summary = "User login", description = "Authenticate user and return JWT token")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Login successful"),
            @ApiResponse(responseCode = "400", description = "Invalid credentials")
    })
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getUsernameOrEmail(),
                            request.getPassword()
                    )
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);
            User user = (User) authentication.getPrincipal();
            user.setLastLogin(LocalDateTime.now());
            userService.save(user);

            Map<String, Object> claims = new HashMap<>();
            claims.put("userId", user.getId());
            claims.put("role", user.getRole().name());
            
            String token = jwtUtil.generateToken(user, claims);

            return ResponseEntity.ok(AuthResponse.builder()
                    .token(token)
                    .type("Bearer")
                    .user(user)
                    .build());

        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Invalid username or password"));
        }
    }

    @Operation(summary = "Get current user", description = "Get information about the currently authenticated user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User information retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Unable to get user information")
    })
    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            User user = (User) authentication.getPrincipal();
            return ResponseEntity.ok(user);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Unable to get user information"));
        }
    }
}
