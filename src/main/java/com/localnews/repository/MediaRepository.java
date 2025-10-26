package com.localnews.repository;

import com.localnews.entity.Media;
import com.localnews.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MediaRepository extends JpaRepository<Media, Long> {
    List<Media> findByUserOrderByUploadedAtDesc(User user);
    List<Media> findAllByOrderByUploadedAtDesc();
}
