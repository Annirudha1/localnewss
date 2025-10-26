package com.localnews.repository;

import com.localnews.entity.Comment;
import com.localnews.entity.Media;
import com.localnews.entity.User;
import com.localnews.entity.Video;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {

    // Video-based methods (for news video platform)
    List<Comment> findByVideoAndIsVisibleToUserTrueOrderByCreatedAtDesc(Video video);

    // Find user's comments for a specific video
    List<Comment> findByVideoAndUserOrderByCreatedAtDesc(Video video, User user);

    // Find all comments by video (for admin view)
    List<Comment> findByVideoOrderByCreatedAtDesc(Video video);

    // Count visible comments for a video
    Long countByVideoAndIsVisibleToUserTrue(Video video);

    // Count all comments for a video (for admin)
    Long countByVideo(Video video);

    // Delete all comments by video (for admin video deletion)
    void deleteByVideo(Video video);

    // User-based methods
    List<Comment> findByUserOrderByCreatedAtDesc(User user);

    // Admin queries - get all comments for a video (including hidden ones)
    @Query("SELECT c FROM Comment c WHERE c.video = :video ORDER BY c.createdAt DESC")
    List<Comment> findAllCommentsByVideoForAdmin(@Param("video") Video video);

    // Find comments by video with active filter
    @Query("SELECT c FROM Comment c WHERE c.video = :video AND c.isActive = true ORDER BY c.createdAt DESC")
    List<Comment> findActiveCommentsByVideo(Video video);


    // Legacy Media-based methods (for backward compatibility with MediaController)
    // Note: These will return empty results since Comments are now linked to Videos, not Media
    @Query("SELECT c FROM Comment c WHERE 1=0 ORDER BY c.createdAt DESC")
    List<Comment> findByMediaOrderByCreatedAtDesc(Media media);

    @Query("SELECT COUNT(c) FROM Comment c WHERE 1=0")
    Long countByMedia(Media media);

    @Query("SELECT c FROM Comment c WHERE c.video = :video ORDER BY c.createdAt DESC")
    List<Comment> findCommentsByVideo(Video video);

}
