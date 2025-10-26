package com.localnews.repository;

import com.localnews.entity.District;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DistrictRepository extends JpaRepository<District, Long> {

    Optional<District> findByCode(String code);

    List<District> findByIsActiveTrue();

    @Query("SELECT d FROM District d WHERE d.isActive = true ORDER BY d.name")
    List<District> findAllActiveDistricts();
}
