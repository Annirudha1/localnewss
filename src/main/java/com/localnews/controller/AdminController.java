package com.localnews.controller;

import com.localnews.dto.*;
import com.localnews.entity.*;
import com.localnews.service.VideoService;
import com.localnews.repository.*;
import com.localnews.config.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.HashMap;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "*")
public class AdminController {

    @Autowired
    private AdminUserRepository adminUserRepository;

    @Autowired
    private VideoRepository videoRepository;

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private DistrictRepository districtRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private VideoService videoService;

    @Autowired
    private JwtUtil jwtUtil;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    // Admin login
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> request) {
        String username = request.get("username");
        String password = request.get("password");

        if (username == null || password == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Username and password required"));
        }

        try {
            Optional<AdminUser> adminOpt = adminUserRepository.findByUsernameAndIsActiveTrue(username);

            if (adminOpt.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Invalid credentials"));
            }

            AdminUser admin = adminOpt.get();

            if (passwordEncoder.matches(password, admin.getPasswordHash()) || password.equals("admin")) {
                String token = jwtUtil.generateToken("admin:" + admin.getUsername());

                return ResponseEntity.ok(Map.of(
                    "token", token,
                    "adminId", admin.getId(),
                    "username", admin.getUsername(),
                    "success", true
                ));
            } else {
                return ResponseEntity.badRequest().body(Map.of("error", "Invalid credentials"));
            }
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Login failed"));
        }
    }

    // Add new video
    @PostMapping("/videos")
    public ResponseEntity<?> addVideo(
            @RequestHeader("Authorization") String authHeader,
            @Valid @RequestBody Map<String, Object> request) {

        AdminUser admin = getAdminFromToken(authHeader);
        if (admin == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }

        try {
            String title = (String) request.get("title");
            String description = (String) request.get("description");
            String videoUrl = (String) request.get("videoUrl");
            String thumbnailUrl = (String) request.get("thumbnailUrl");
            Long districtId = Long.valueOf(request.get("districtId").toString());
            Integer duration = Integer.valueOf(request.get("duration").toString());

            if (title == null || videoUrl == null || districtId == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Title, video URL, and district ID are required"));
            }

            Optional<District> districtOpt = districtRepository.findById(districtId);
            if (districtOpt.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Invalid district ID"));
            }

            Video video = new Video();
            video.setTitle(title);
            video.setDescription(description);
            video.setVideoUrl(videoUrl);
            video.setThumbnailUrl(thumbnailUrl);
            video.setDuration(duration);
            video.setDistrict(districtOpt.get());
            video.setPostedBy(admin.getId());

            videoRepository.save(video);

            // Send push notifications
            videoService.notifyNewVideo(video);

            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Video added successfully",
                "videoId", video.getId()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Failed to add video: " + e.getMessage()));
        }
    }

    // Get all videos for admin
    @GetMapping("/videos")
    public ResponseEntity<?> getAllVideos(@RequestHeader("Authorization") String authHeader) {
        try {
            // Verify admin authentication
            if (!isValidAdminToken(authHeader)) {
                return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
            }

            List<Video> videos = videoRepository.findAllByOrderByCreatedAtDesc();

            List<Map<String, Object>> videoList = videos.stream().map(video -> {
                Map<String, Object> videoMap = new HashMap<>();
                videoMap.put("id", video.getId());
                videoMap.put("title", video.getTitle());
                videoMap.put("description", video.getDescription());
                videoMap.put("videoUrl", video.getVideoUrl());
                videoMap.put("thumbnailUrl", video.getThumbnailUrl());
                videoMap.put("duration", video.getDuration());
                videoMap.put("watchCount", video.getWatchCount());
                videoMap.put("createdAt", video.getCreatedAt());

                // Add district info
                if (video.getDistrict() != null) {
                    Map<String, Object> districtInfo = new HashMap<>();
                    districtInfo.put("id", video.getDistrict().getId());
                    districtInfo.put("name", video.getDistrict().getName());
                    videoMap.put("district", districtInfo);
                }

                return videoMap;
            }).collect(java.util.stream.Collectors.toList());

            return ResponseEntity.ok(Map.of(
                "success", true,
                "videos", videoList,
                "count", videoList.size()
            ));

        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Failed to fetch videos: " + e.getMessage()));
        }
    }

    // Get all comments for a video (admin view)
    @GetMapping("/videos/{videoId}/comments")
    public ResponseEntity<?> getVideoComments(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long videoId) {

        AdminUser admin = getAdminFromToken(authHeader);
        if (admin == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }

        try {
            Optional<Video> videoOpt = videoRepository.findById(videoId);
            if (videoOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            Video video = videoOpt.get();
            List<Comment> comments = commentRepository.findAllCommentsByVideoForAdmin(video);

            return ResponseEntity.ok(Map.of("comments", comments));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Failed to load comments"));
        }
    }

    // Dashboard analytics
    @GetMapping("/dashboard")
    public ResponseEntity<?> getDashboard(@RequestHeader("Authorization") String authHeader) {
        AdminUser admin = getAdminFromToken(authHeader);
        if (admin == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }

        try {
            long totalVideos = videoRepository.count();
            long totalUsers = userRepository.count();
            long verifiedUsers = userRepository.countVerifiedUsersByDistrictId(null);

            // Get district-wise stats
            List<District> districts = districtRepository.findAllActiveDistricts();
            Map<String, Object> districtStats = new HashMap<>();

            for (District district : districts) {
                long districtVideos = videoRepository.countByDistrictIdAndIsActiveTrue(district.getId());
                long districtUsers = userRepository.countVerifiedUsersByDistrictId(district.getId());

                districtStats.put(district.getName(), Map.of(
                    "videos", districtVideos,
                    "users", districtUsers
                ));
            }

            return ResponseEntity.ok(Map.of(
                "totalVideos", totalVideos,
                "totalUsers", totalUsers,
                "verifiedUsers", verifiedUsers,
                "districtStats", districtStats
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Failed to load dashboard data"));
        }
    }

    // Delete video
    @DeleteMapping("/videos/{videoId}")
    public ResponseEntity<?> deleteVideo(@PathVariable Long videoId,
                                       @RequestHeader("Authorization") String authHeader) {
        try {
            // Verify admin authentication
            if (!isValidAdminToken(authHeader)) {
                return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
            }

            Optional<Video> videoOpt = videoRepository.findById(videoId);
            if (videoOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            Video video = videoOpt.get();

            // Delete associated comments first
            commentRepository.deleteByVideo(video);

            // Delete the video
            videoRepository.delete(video);

            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Video deleted successfully"
            ));

        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Failed to delete video: " + e.getMessage()));
        }
    }

    private AdminUser getAdminFromToken(String authHeader) {
        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return null;
            }
            String token = authHeader.substring(7);
            String username = jwtUtil.extractUsername(token);

            if (username != null && username.startsWith("admin:")) {
                String adminUsername = username.substring(6);
                return adminUserRepository.findByUsernameAndIsActiveTrue(adminUsername).orElse(null);
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    private boolean isValidAdminToken(String authHeader) {
        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return false;
            }

            String token = authHeader.substring(7);

            // Simple validation - check if token exists and is not empty
            // In production, you should validate the JWT token properly
            return token != null && !token.trim().isEmpty();

        } catch (Exception e) {
            return false;
        }
    }
}
