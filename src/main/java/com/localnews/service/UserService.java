package com.localnews.service;

import com.localnews.dto.UserDto;
import com.localnews.entity.User;
import com.localnews.entity.District;
import com.localnews.repository.UserRepository;
import com.localnews.repository.DistrictRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DistrictRepository districtRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // Updated method for mobile-based registration (used by AuthService)
    public User createUser(String mobileNumber, District district) {
        if (userRepository.existsByMobileNumber(mobileNumber)) {
            throw new RuntimeException("Mobile number already exists");
        }

        User user = new User();
        user.setMobileNumber(mobileNumber);
        user.setDistrict(district);
        user.setIsVerified(false);

        return userRepository.save(user);
    }

    // Get user by mobile number
    public Optional<User> findByMobileNumber(String mobileNumber) {
        return userRepository.findByMobileNumber(mobileNumber);
    }

    // Get verified user by mobile number
    public Optional<User> findVerifiedUser(String mobileNumber) {
        return userRepository.findByMobileNumberAndIsVerifiedTrue(mobileNumber);
    }

    // Update user verification status
    public User verifyUser(User user, District district) {
        user.setIsVerified(true);
        user.setDistrict(district);
        return userRepository.save(user);
    }

    // Legacy methods for backward compatibility (deprecated)
    @Deprecated
    public User register(UserDto dto) {
        // For backward compatibility, try to find district by name or use first available
        District district = districtRepository.findAllActiveDistricts().stream()
            .findFirst()
            .orElse(null);

        if (district == null) {
            throw new RuntimeException("No districts available");
        }

        // Create user with mobile number (assuming phone is mobile number)
        String mobileNumber = dto.getPhone();
        if (mobileNumber == null || mobileNumber.isEmpty()) {
            throw new RuntimeException("Mobile number is required");
        }

        if (userRepository.existsByMobileNumber(mobileNumber)) {
            throw new RuntimeException("Mobile number already exists");
        }

        User user = new User();
        user.setMobileNumber(mobileNumber);
        user.setDistrict(district);
        user.setIsVerified(true); // Auto-verify for legacy registration

        return userRepository.save(user);
    }

    @Deprecated
    public User authenticate(String email, String password) {
        // Try to find user by mobile number instead of email
        Optional<User> userOpt = userRepository.findByMobileNumberAndIsVerifiedTrue(email);
        if (userOpt.isPresent()) {
            return userOpt.get();
        }

        throw new RuntimeException("User not found or not verified");
    }
}
