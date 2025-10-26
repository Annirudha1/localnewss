package com.localnews.repository;

import com.localnews.entity.Video;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VideoRepository extends JpaRepository<Video, Long> {

    // Find all videos ordered by creation date (newest first)
    List<Video> findAllByOrderByCreatedAtDesc();

    // Find videos by district and active status, ordered by creation date
    Page<Video> findByDistrictIdAndIsActiveTrueOrderByCreatedAtDesc(Long districtId, Pageable pageable);

    // Find all active videos ordered by creation date
    @Query("SELECT v FROM Video v WHERE v.isActive = true ORDER BY v.createdAt DESC")
    Page<Video> findAllActiveVideosOrderByCreatedAtDesc(Pageable pageable);

    // Count videos by district and active status
    long countByDistrictIdAndIsActiveTrue(Long districtId);

    // Find active videos by district
    List<Video> findByDistrictIdAndIsActiveTrueOrderByCreatedAtDesc(Long districtId);

    // Find videos by user/poster
    List<Video> findByPostedByOrderByCreatedAtDesc(Long postedBy);

    // Find all active videos
    List<Video> findByIsActiveTrueOrderByCreatedAtDesc();

    // Custom queries for admin dashboard
    @Query("SELECT COUNT(v) FROM Video v WHERE v.isActive = true")
    long countActiveVideos();

    @Query("SELECT v FROM Video v WHERE v.isActive = true AND v.district.id = :districtId ORDER BY v.createdAt DESC")
    List<Video> findActiveVideosByDistrict(@Param("districtId") Long districtId);
}
