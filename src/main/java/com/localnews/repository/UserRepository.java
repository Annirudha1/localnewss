package com.localnews.repository;

import com.localnews.entity.User;
import com.localnews.entity.District;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // Mobile-based authentication methods
    Optional<User> findByMobileNumber(String mobileNumber);

    boolean existsByMobileNumber(String mobileNumber);

    Optional<User> findByMobileNumberAndIsVerifiedTrue(String mobileNumber);

    // OTP verification methods
    @Query("SELECT u FROM User u WHERE u.mobileNumber = :mobileNumber AND u.otp = :otp AND u.otpExpiry > :currentTime")
    Optional<User> findByMobileNumberAndValidOtp(@Param("mobileNumber") String mobileNumber,
                                                @Param("otp") String otp,
                                                @Param("currentTime") LocalDateTime currentTime);

    // District-based queries
    List<User> findByDistrictAndIsVerifiedTrue(District district);

    @Query("SELECT COUNT(u) FROM User u WHERE u.district.id = :districtId AND u.isVerified = true")
    Long countVerifiedUsersByDistrictId(@Param("districtId") Long districtId);
}
