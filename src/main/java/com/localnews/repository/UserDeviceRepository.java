package com.localnews.repository;

import com.localnews.entity.UserDevice;
import com.localnews.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserDeviceRepository extends JpaRepository<UserDevice, Long> {

    Optional<UserDevice> findByUserAndDeviceToken(User user, String deviceToken);

    List<UserDevice> findByUserAndIsActiveTrue(User user);

    @Query("SELECT ud FROM UserDevice ud WHERE ud.user = :user AND ud.isActive = true")
    List<UserDevice> findActiveDevicesByUser(@Param("user") User user);

    @Query("SELECT ud FROM UserDevice ud WHERE ud.isActive = true")
    List<UserDevice> findAllActiveDevices();
}
