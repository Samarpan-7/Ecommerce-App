package ecommerce.example.Death_Mart.controller;

import ecommerce.example.Death_Mart.dto.request.LoginRequest;
import ecommerce.example.Death_Mart.dto.request.RegisterRequest;
import ecommerce.example.Death_Mart.dto.response.ApiResponse;
import ecommerce.example.Death_Mart.dto.response.AuthResponse;
import ecommerce.example.Death_Mart.dto.response.UserResponse;
import ecommerce.example.Death_Mart.security.CurrentUser;
import ecommerce.example.Death_Mart.security.UserPrincipal;
import ecommerce.example.Death_Mart.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<UserResponse>> register(@Valid @RequestBody RegisterRequest request) {
        UserResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("User registered successfully", response));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success("Login successful", response));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> getCurrentUser(@CurrentUser UserPrincipal currentUser) {
        UserResponse response = authService.getCurrentUser(currentUser);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
