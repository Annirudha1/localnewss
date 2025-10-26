package com.localnews.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_video_interactions",
       uniqueConstraints = @UniqueConstraint(name = "unique_user_video", columnNames = {"user_id", "video_id"}),
       indexes = {
           @Index(name = "idx_user_video", columnList = "user_id, video_id")
       })
public class UserVideoInteraction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "video_id", nullable = false)
    private Video video;

    @Column(name = "has_liked")
    private Boolean hasLiked = false;

    @Column(name = "has_watched")
    private Boolean hasWatched = false;

    @Column(name = "watch_duration")
    private Integer watchDuration = 0; // in seconds

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    // Constructors
    public UserVideoInteraction() {}

    public UserVideoInteraction(User user, Video video) {
        this.user = user;
        this.video = video;
    }

    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public Video getVideo() { return video; }
    public void setVideo(Video video) { this.video = video; }

    public Boolean getHasLiked() { return hasLiked; }
    public void setHasLiked(Boolean hasLiked) { this.hasLiked = hasLiked; }

    public Boolean getHasWatched() { return hasWatched; }
    public void setHasWatched(Boolean hasWatched) { this.hasWatched = hasWatched; }

    public Integer getWatchDuration() { return watchDuration; }
    public void setWatchDuration(Integer watchDuration) { this.watchDuration = watchDuration; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
