package com.localnews.dto;

import java.time.LocalDateTime;

public class VideoDto {

    private Long id;
    private String title;
    private String description;
    private String videoUrl;
    private String thumbnailUrl;
    private Integer duration;
    private String districtName;
    private Long districtId;
    private Integer likeCount;
    private Integer watchCount;
    private boolean hasLiked;
    private boolean hasWatched;
    private LocalDateTime createdAt;
    private Integer commentCount;

    // Constructors
    public VideoDto() {}

    public VideoDto(Long id, String title, String videoUrl, String thumbnailUrl,
                   Integer duration, String districtName, Integer likeCount,
                   Integer watchCount, LocalDateTime createdAt) {
        this.id = id;
        this.title = title;
        this.videoUrl = videoUrl;
        this.thumbnailUrl = thumbnailUrl;
        this.duration = duration;
        this.districtName = districtName;
        this.likeCount = likeCount;
        this.watchCount = watchCount;
        this.createdAt = createdAt;
    }

    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getVideoUrl() { return videoUrl; }
    public void setVideoUrl(String videoUrl) { this.videoUrl = videoUrl; }

    public String getThumbnailUrl() { return thumbnailUrl; }
    public void setThumbnailUrl(String thumbnailUrl) { this.thumbnailUrl = thumbnailUrl; }

    public Integer getDuration() { return duration; }
    public void setDuration(Integer duration) { this.duration = duration; }

    public String getDistrictName() { return districtName; }
    public void setDistrictName(String districtName) { this.districtName = districtName; }

    public Long getDistrictId() { return districtId; }
    public void setDistrictId(Long districtId) { this.districtId = districtId; }

    public Integer getLikeCount() { return likeCount; }
    public void setLikeCount(Integer likeCount) { this.likeCount = likeCount; }

    public Integer getWatchCount() { return watchCount; }
    public void setWatchCount(Integer watchCount) { this.watchCount = watchCount; }

    public boolean isHasLiked() { return hasLiked; }
    public void setHasLiked(boolean hasLiked) { this.hasLiked = hasLiked; }

    public boolean isHasWatched() { return hasWatched; }
    public void setHasWatched(boolean hasWatched) { this.hasWatched = hasWatched; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public Integer getCommentCount() { return commentCount; }
    public void setCommentCount(Integer commentCount) { this.commentCount = commentCount; }
}
