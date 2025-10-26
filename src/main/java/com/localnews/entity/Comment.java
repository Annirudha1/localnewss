package com.localnews.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "comments", indexes = {
    @Index(name = "idx_video_user", columnList = "video_id, user_id"),
    @Index(name = "idx_user_video", columnList = "user_id, video_id")
})
public class Comment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "video_id", nullable = false)
    private Video video;

    @Column(name = "comment_text", nullable = false, columnDefinition = "TEXT")
    private String commentText;

    @Column(name = "is_visible_to_user")
    private Boolean isVisibleToUser = true;

    private boolean isActive;  // <-- Add this field
    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    // Constructors
    public Comment() {}

    public Comment(User user, Video video, String commentText) {
        this.user = user;
        this.video = video;
        this.commentText = commentText;
    }

    // Legacy constructor for backward compatibility with MediaController
    @Deprecated
    public Comment(String content, Media media, User user) {
        this.user = user;
        this.commentText = content;
        // Note: This constructor is for legacy Media support only
        // The video field will remain null for Media-based comments
    }

    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public Video getVideo() { return video; }
    public void setVideo(Video video) { this.video = video; }

    public String getCommentText() { return commentText; }
    public void setCommentText(String commentText) { this.commentText = commentText; }

    public Boolean getIsVisibleToUser() { return isVisibleToUser; }
    public void setIsVisibleToUser(Boolean isVisibleToUser) { this.isVisibleToUser = isVisibleToUser; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    // Legacy methods for backward compatibility (can be removed later)
    @Deprecated
    public String getContent() { return commentText; }

    @Deprecated
    public void setContent(String content) { this.commentText = content; }

    @Deprecated
    public Media getMedia() { return null; }

    public String getUserName() {
        return user != null ? user.getMobileNumber() : "Unknown";
    }
}
