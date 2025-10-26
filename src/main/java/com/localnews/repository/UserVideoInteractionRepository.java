package com.localnews.repository;

import com.localnews.entity.UserVideoInteraction;
import com.localnews.entity.User;
import com.localnews.entity.Video;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

@Repository
public interface UserVideoInteractionRepository extends JpaRepository<UserVideoInteraction, Long> {

    Optional<UserVideoInteraction> findByUserAndVideo(User user, Video video);

    // Get user's liked videos
    @Query("SELECT uvi FROM UserVideoInteraction uvi WHERE uvi.user = :user AND uvi.hasLiked = true ORDER BY uvi.updatedAt DESC")
    List<UserVideoInteraction> findLikedVideosByUser(@Param("user") User user);

    // Get user's watched videos
    @Query("SELECT uvi FROM UserVideoInteraction uvi WHERE uvi.user = :user AND uvi.hasWatched = true ORDER BY uvi.updatedAt DESC")
    List<UserVideoInteraction> findWatchedVideosByUser(@Param("user") User user);

    // Count likes for a video
    @Query("SELECT COUNT(uvi) FROM UserVideoInteraction uvi WHERE uvi.video = :video AND uvi.hasLiked = true")
    Long countLikesByVideo(@Param("video") Video video);

    // Count watches for a video
    @Query("SELECT COUNT(uvi) FROM UserVideoInteraction uvi WHERE uvi.video = :video AND uvi.hasWatched = true")
    Long countWatchesByVideo(@Param("video") Video video);
}
