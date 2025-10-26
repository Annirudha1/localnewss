package com.localnews.service;

import com.localnews.dto.AuthResponse;
import com.localnews.dto.SendOtpRequest;
import com.localnews.dto.VerifyOtpRequest;
import com.localnews.entity.District;
import com.localnews.entity.User;
import com.localnews.repository.DistrictRepository;
import com.localnews.repository.UserRepository;
import com.localnews.config.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Random;

@Service
public class AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DistrictRepository districtRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private SmsService smsService;

    private final Random random = new Random();

    @Transactional
    public AuthResponse sendOtp(SendOtpRequest request) {
        try {
            String mobileNumber = request.getMobileNumber();

            // Generate 6-digit OTP
            String otp = String.format("%06d", random.nextInt(1000000));
            LocalDateTime otpExpiry = LocalDateTime.now().plusMinutes(10); // 10 minutes validity

            // Find or create user
            User user = userRepository.findByMobileNumber(mobileNumber)
                    .orElse(new User());

            user.setMobileNumber(mobileNumber);
            user.setOtp(otp);
            user.setOtpExpiry(otpExpiry);

            userRepository.save(user);

            // Send OTP via SMS (implement based on your SMS provider)
            boolean smsSent = smsService.sendOtp(mobileNumber, otp);

            if (smsSent) {
                return new AuthResponse(true, "OTP sent successfully to " + mobileNumber);
            } else {
                return new AuthResponse(false, "Failed to send OTP. Please try again.");
            }

        } catch (Exception e) {
            return new AuthResponse(false, "Error sending OTP: " + e.getMessage());
        }
    }

    @Transactional
    public AuthResponse verifyOtp(VerifyOtpRequest request) {
        try {
            String mobileNumber = request.getMobileNumber();
            String otp = request.getOtp();
            Long districtId = request.getDistrictId();

            // Find user by mobile number
            Optional<User> userOpt = userRepository.findByMobileNumber(mobileNumber);

            if (userOpt.isEmpty()) {
                return new AuthResponse(false, "No OTP request found for this mobile number");
            }

            User user = userOpt.get();

            // For testing - accept any 6-digit OTP
            if (otp == null || otp.length() != 6 || !otp.matches("\\d{6}")) {
                return new AuthResponse(false, "Please enter a valid 6-digit OTP");
            }

            // In production, uncomment this for actual OTP verification:
            // if (!otp.equals(user.getOtp()) || user.getOtpExpiry() == null ||
            //     user.getOtpExpiry().isBefore(LocalDateTime.now())) {
            //     return new AuthResponse(false, "Invalid or expired OTP");
            // }

            // Verify district exists
            Optional<District> districtOpt = districtRepository.findById(districtId);
            if (districtOpt.isEmpty()) {
                return new AuthResponse(false, "Invalid district selected");
            }

            District district = districtOpt.get();

            // Update user verification status and district
            user.setIsVerified(true);
            user.setDistrict(district);
            user.setOtp(null); // Clear OTP after successful verification
            user.setOtpExpiry(null);

            userRepository.save(user);

            // Generate JWT token
            String token = jwtUtil.generateToken(user.getMobileNumber());

            return new AuthResponse(token, user.getId(), user.getMobileNumber(), district.getName());

        } catch (Exception e) {
            return new AuthResponse(false, "Error verifying OTP: " + e.getMessage());
        }
    }

    public User getUserFromToken(String token) {
        try {
            String mobileNumber = jwtUtil.extractUsername(token);
            return userRepository.findByMobileNumberAndIsVerifiedTrue(mobileNumber).orElse(null);
        } catch (Exception e) {
            return null;
        }
    }

    public boolean validateToken(String token) {
        try {
            String mobileNumber = jwtUtil.extractUsername(token);
            User user = userRepository.findByMobileNumberAndIsVerifiedTrue(mobileNumber).orElse(null);
            return user != null && jwtUtil.validateToken(token);
        } catch (Exception e) {
            return false;
        }
    }
}
