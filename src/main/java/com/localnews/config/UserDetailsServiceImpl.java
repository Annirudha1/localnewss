package com.localnews.config;

import com.localnews.entity.User;
import com.localnews.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // In the new system, username can be mobile number or admin username

        // First try to find by mobile number (for regular users)
        User user = userRepository.findByMobileNumberAndIsVerifiedTrue(username)
                .orElse(null);

        if (user != null) {
            List<GrantedAuthority> authorities = Collections.singletonList(
                    new SimpleGrantedAuthority("ROLE_USER")
            );

            // For mobile users, we use mobile number as both username and password placeholder
            // since actual authentication is done via OTP
            return new org.springframework.security.core.userdetails.User(
                    user.getMobileNumber(),
                    "N/A", // Password not used in OTP system
                    user.getIsVerified(),
                    true, // Account non-expired
                    true, // Credentials non-expired
                    true, // Account non-locked
                    authorities
            );
        }

        // If not found as mobile user, check if it's an admin username
        if (username.startsWith("admin:")) {
            // This is handled by AdminUser authentication in AuthService
            // For now, return a basic user details for admin
            List<GrantedAuthority> authorities = Collections.singletonList(
                    new SimpleGrantedAuthority("ROLE_ADMIN")
            );

            return new org.springframework.security.core.userdetails.User(
                    username,
                    "N/A", // Password handled separately in admin authentication
                    authorities
            );
        }

        throw new UsernameNotFoundException("User not found: " + username);
    }
}
