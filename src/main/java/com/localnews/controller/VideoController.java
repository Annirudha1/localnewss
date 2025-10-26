package com.localnews.controller;

import com.localnews.dto.*;
import com.localnews.entity.Comment;
import com.localnews.entity.User;
import com.localnews.entity.Video;
import com.localnews.entity.UserDevice;
import com.localnews.service.AuthService;
import com.localnews.service.VideoService;
import com.localnews.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.HashMap;

@RestController
@RequestMapping("/api/videos")
@CrossOrigin(origins = "*")
public class VideoController {

    @Autowired
    private VideoService videoService;

    @Autowired
    private AuthService authService;

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private VideoRepository videoRepository;

    @Autowired
    private UserDeviceRepository userDeviceRepository;

    // Get video feed (LRU order)
    @GetMapping("/feed")
    public ResponseEntity<?> getVideoFeed(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        User user = getUserFromToken(authHeader);
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Invalid token"));
        }

        try {
            Page<VideoDto> videos = videoService.getVideoFeed(user, page, size);
            return ResponseEntity.ok(Map.of(
                "videos", videos.getContent(),
                "totalElements", videos.getTotalElements(),
                "totalPages", videos.getTotalPages(),
                "currentPage", page
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Failed to load videos"));
        }
    }

    // Get specific video
    @GetMapping("/{videoId}")
    public ResponseEntity<?> getVideo(
            @PathVariable Long videoId,
            @RequestHeader("Authorization") String authHeader) {

        User user = getUserFromToken(authHeader);
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Invalid token"));
        }

        Optional<VideoDto> video = videoService.getVideoById(videoId, user);
        if (video.isPresent()) {
            return ResponseEntity.ok(video.get());
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    // Record video watch
    @PostMapping("/{videoId}/watch")
    public ResponseEntity<?> recordWatch(
            @PathVariable Long videoId,
            @RequestHeader("Authorization") String authHeader,
            @RequestBody Map<String, Integer> request) {

        User user = getUserFromToken(authHeader);
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Invalid token"));
        }

        Integer watchDuration = request.get("watchDuration");
        if (watchDuration == null || watchDuration < 0) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid watch duration"));
        }

        boolean recorded = videoService.recordWatch(videoId, user, watchDuration);
        if (recorded) {
            return ResponseEntity.ok(Map.of("success", true, "message", "Watch recorded"));
        } else {
            return ResponseEntity.badRequest().body(Map.of("error", "Failed to record watch"));
        }
    }

    // Like/unlike video
    @PostMapping("/{videoId}/like")
    public ResponseEntity<?> toggleLike(
            @PathVariable Long videoId,
            @RequestHeader("Authorization") String authHeader) {

        User user = getUserFromToken(authHeader);
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Invalid token"));
        }

        VideoDto video = videoService.toggleLike(videoId, user);
        if (video != null) {
            return ResponseEntity.ok(Map.of(
                "success", true,
                "likeCount", video.getLikeCount(),
                "hasLiked", video.isHasLiked()
            ));
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    // Add comment
    @PostMapping("/{videoId}/comments")
    public ResponseEntity<?> addComment(
            @PathVariable Long videoId,
            @RequestHeader("Authorization") String authHeader,
            @Valid @RequestBody CommentRequest request) {

        User user = getUserFromToken(authHeader);
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Invalid token"));
        }

        try {
            Optional<Video> videoOpt = videoRepository.findById(videoId);
            if (videoOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            Video video = videoOpt.get();
            Comment comment = new Comment(user, video, request.getCommentText());
            commentRepository.save(comment);

            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Comment added successfully",
                "commentId", comment.getId()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Failed to add comment"));
        }
    }

    // Get user's comments for a video
    @GetMapping("/{videoId}/my-comments")
    public ResponseEntity<?> getMyComments(
            @PathVariable Long videoId,
            @RequestHeader("Authorization") String authHeader) {

        User user = getUserFromToken(authHeader);
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Invalid token"));
        }

        try {
            Optional<Video> videoOpt = videoRepository.findById(videoId);
            if (videoOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            Video video = videoOpt.get();
            List<Comment> comments = commentRepository.findByVideoAndUserOrderByCreatedAtDesc(video, user);

            return ResponseEntity.ok(Map.of("comments", comments));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Failed to load comments"));
        }
    }

    // Register device for push notifications
    @PostMapping("/user/register-device")
    public ResponseEntity<?> registerDevice(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody Map<String, String> request) {

        User user = getUserFromToken(authHeader);
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Invalid token"));
        }

        String deviceToken = request.get("deviceToken");
        if (deviceToken == null || deviceToken.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Device token is required"));
        }

        try {
            // Check if device already exists
            Optional<UserDevice> existingDevice = userDeviceRepository.findByUserAndDeviceToken(user, deviceToken);

            if (existingDevice.isPresent()) {
                // Reactivate if inactive
                UserDevice device = existingDevice.get();
                device.setIsActive(true);
                userDeviceRepository.save(device);
            } else {
                // Create new device
                UserDevice newDevice = new UserDevice(user, deviceToken);
                userDeviceRepository.save(newDevice);
            }

            return ResponseEntity.ok(Map.of("success", true, "message", "Device registered successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Failed to register device"));
        }
    }

    private User getUserFromToken(String authHeader) {
        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return null;
            }
            String token = authHeader.substring(7);
            return authService.getUserFromToken(token);
        } catch (Exception e) {
            return null;
        }
    }
}
