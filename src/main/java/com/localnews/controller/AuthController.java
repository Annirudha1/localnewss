package com.localnews.controller;

import com.localnews.dto.*;
import com.localnews.entity.District;
import com.localnews.service.AuthService;
import com.localnews.repository.DistrictRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import jakarta.servlet.http.HttpSession;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    @Autowired
    private AuthService authService;

    @Autowired
    private DistrictRepository districtRepository;

    @PostMapping("/send-otp")
    public ResponseEntity<AuthResponse> sendOtp(@Valid @RequestBody SendOtpRequest request) {
        AuthResponse response = authService.sendOtp(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<AuthResponse> verifyOtp(@Valid @RequestBody VerifyOtpRequest request) {
        AuthResponse response = authService.verifyOtp(request);

        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/districts")
    public ResponseEntity<List<District>> getDistricts() {
        List<District> districts = districtRepository.findAllActiveDistricts();
        return ResponseEntity.ok(districts);
    }

    @PostMapping("/validate-token")
    public ResponseEntity<AuthResponse> validateToken(@RequestHeader("Authorization") String authHeader) {
        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.badRequest().body(new AuthResponse(false, "Invalid token format"));
            }

            String token = authHeader.substring(7);
            boolean isValid = authService.validateToken(token);

            if (isValid) {
                return ResponseEntity.ok(new AuthResponse(true, "Token is valid"));
            } else {
                return ResponseEntity.badRequest().body(new AuthResponse(false, "Invalid or expired token"));
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new AuthResponse(false, "Token validation failed"));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpSession session, @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            // Clear session data
            if (session != null) {
                session.removeAttribute("user");
                session.invalidate();
            }

            // Note: For JWT tokens, we rely on client-side token removal since JWT is stateless
            // In a production environment, you might want to implement a token blacklist

            return ResponseEntity.ok(Map.of(
                "status", "SUCCESS",
                "message", "Logged out successfully",
                "redirectTo", "/login"
            ));

        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                "status", "ERROR",
                "message", "Logout failed: " + e.getMessage()
            ));
        }
    }
}
