package com.localnews.controller;

import com.localnews.entity.*;
import com.localnews.repository.*;
import com.localnews.service.MediaService;
import com.localnews.config.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/media")
public class MediaController {

    @Autowired
    private MediaService mediaService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MediaRepository mediaRepository;

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private LikeRepository likeRepository;

    @Autowired
    private DistrictRepository districtRepository;

    @Autowired
    private VideoRepository videoRepository;

    @Autowired
    private AdminUserRepository adminUserRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @PostMapping("/upload")
    public ResponseEntity<?> upload(@RequestPart("file") MultipartFile file,
                                   @RequestPart(value = "title", required = false) String title,
                                   HttpSession session) {
        try {
            User user = null;

            // First try to get user from session (UI login)
            user = (User) session.getAttribute("user");

            // If no session user, try JWT authentication (API login)
            if (user == null) {
                Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

                if (authentication == null || !authentication.isAuthenticated()) {
                    return ResponseEntity.status(401).body(Map.of(
                        "error", "User not authenticated. Please login first.",
                        "status", "UNAUTHORIZED"
                    ));
                }

                String username = authentication.getName();
                // Try to find user by mobile number (new system) or fallback for legacy
                user = userRepository.findByMobileNumberAndIsVerifiedTrue(username)
                        .orElseThrow(() -> new RuntimeException("User not found: " + username));
            }

            // Validate file
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "error", "File is empty",
                    "status", "VALIDATION_ERROR"
                ));
            }

            // Check file size (10MB limit)
            long maxSize = 10 * 1024 * 1024; // 10MB
            if (file.getSize() > maxSize) {
                return ResponseEntity.badRequest().body(Map.of(
                    "error", "File size exceeds 10MB limit",
                    "status", "VALIDATION_ERROR"
                ));
            }

            // Check file type
            String contentType = file.getContentType();
            if (contentType == null || (!contentType.startsWith("image/") && !contentType.startsWith("video/"))) {
                return ResponseEntity.badRequest().body(Map.of(
                    "error", "Only image and video files are allowed",
                    "status", "VALIDATION_ERROR"
                ));
            }

            // Use provided title or default to filename
            String mediaTitle = (title != null && !title.trim().isEmpty()) ? title.trim() : file.getOriginalFilename();

            String path = mediaService.uploadFile(file, user, mediaTitle);

            return ResponseEntity.ok(Map.of(
                "message", "File uploaded successfully",
                "path", path,
                "title", mediaTitle,
                "fileName", file.getOriginalFilename(),
                "fileSize", file.getSize(),
                "contentType", contentType,
                "uploaderName", user.getName(),
                "status", "SUCCESS"
            ));

        } catch (IOException e) {
            return ResponseEntity.status(500).body(Map.of(
                "error", "File upload failed: " + e.getMessage(),
                "status", "UPLOAD_ERROR"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", e.getMessage(),
                "status", "ERROR"
            ));
        }
    }

    @GetMapping("/list")
    public ResponseEntity<?> getUserMediaList(HttpSession session) {
        try {
            System.out.println("MediaController: Starting getUserMediaList request");

            User user = (User) session.getAttribute("user");
            System.out.println("User from session: " + (user != null ? user.getEmail() : "null"));

            if (user == null) {
                Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                System.out.println("Authentication: " + (authentication != null ? authentication.getName() : "null"));

                if (authentication == null || !authentication.isAuthenticated()) {
                    System.out.println("User not authenticated - returning 401");
                    return ResponseEntity.status(401).body(Map.of(
                        "error", "User not authenticated",
                        "status", "UNAUTHORIZED"
                    ));
                }
                String username = authentication.getName();
                // Try to find user by mobile number (new system) or fallback for legacy
                user = userRepository.findByMobileNumberAndIsVerifiedTrue(username)
                        .orElseThrow(() -> new RuntimeException("User not found: " + username));
                System.out.println("User found via authentication: " + user.getEmail());
            }

            final User currentUser = user; // Create final reference for lambda
            System.out.println("Fetching media for user: " + currentUser.getEmail());

            List<Media> mediaList = mediaService.getAllMediaByUser(user);
            System.out.println("Media count found: " + mediaList.size());

            List<Map<String, Object>> mediaData = mediaList.stream().map(media -> {
                Map<String, Object> mediaMap = new HashMap<>();
                mediaMap.put("id", media.getId());
                mediaMap.put("title", media.getTitle());
                mediaMap.put("filename", media.getFilename());
                mediaMap.put("fileType", media.getFileType());
                mediaMap.put("fileSize", media.getFileSize());
                mediaMap.put("uploadedAt", media.getUploadedAt().toString());
                mediaMap.put("thumbnailPath", media.getThumbnailPath() != null ? media.getThumbnailPath() : "");
                mediaMap.put("uploaderName", media.getUser().getName());

                // Add interaction counts
                try {
                    mediaMap.put("likeCount", likeRepository.countLikesByMedia(media));
                    mediaMap.put("dislikeCount", likeRepository.countDislikesByMedia(media));
                    mediaMap.put("commentCount", commentRepository.countByMedia(media));

                    // Add user's reaction if user is logged in
                    if (currentUser != null) {
                        Optional<Like> userLike = likeRepository.findByMediaAndUser(media, currentUser);
                        String userReaction = userLike.isPresent() ? userLike.get().getType().toString().toLowerCase() : "none";
                        mediaMap.put("userReaction", userReaction);
                    } else {
                        mediaMap.put("userReaction", "none");
                    }
                } catch (Exception e) {
                    System.err.println("Error fetching interaction data for media " + media.getId() + ": " + e.getMessage());
                    mediaMap.put("likeCount", 0);
                    mediaMap.put("dislikeCount", 0);
                    mediaMap.put("commentCount", 0);
                    mediaMap.put("userReaction", "none");
                }

                return mediaMap;
            }).collect(Collectors.toList());

            Map<String, Object> response = new HashMap<>();
            response.put("media", mediaData);
            response.put("count", mediaList.size());
            response.put("status", "SUCCESS");

            System.out.println("Successfully returning " + mediaList.size() + " media items");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("Error in getUserMediaList: " + e.getMessage());
            e.printStackTrace();

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to load media: " + e.getMessage());
            errorResponse.put("status", "ERROR");
            errorResponse.put("details", e.getClass().getSimpleName());

            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    @GetMapping("/all")
    public ResponseEntity<?> getAllMediaList(HttpSession session) {
        try {
            System.out.println("MediaController: Starting getAllMediaList request");

            // Get current user for user-specific reactions
            User currentUser = getAuthenticatedUser(session);
            System.out.println("Current user: " + (currentUser != null ? currentUser.getEmail() : "anonymous"));

            List<Media> mediaList = mediaService.getAllMedia();
            System.out.println("All media count found: " + mediaList.size());

            List<Map<String, Object>> mediaData = mediaList.stream().map(media -> {
                Map<String, Object> mediaMap = new HashMap<>();
                mediaMap.put("id", media.getId());
                mediaMap.put("title", media.getTitle());
                mediaMap.put("filename", media.getFilename());
                mediaMap.put("fileType", media.getFileType());
                mediaMap.put("fileSize", media.getFileSize());
                mediaMap.put("uploadedAt", media.getUploadedAt().toString());
                mediaMap.put("thumbnailPath", media.getThumbnailPath() != null ? media.getThumbnailPath() : "");
                mediaMap.put("uploaderName", media.getUser().getName());

                // Add interaction counts for all media list
                try {
                    mediaMap.put("likeCount", likeRepository.countLikesByMedia(media));
                    mediaMap.put("dislikeCount", likeRepository.countDislikesByMedia(media));
                    mediaMap.put("commentCount", commentRepository.countByMedia(media));

                    // Add user's reaction if user is logged in
                    if (currentUser != null) {
                        Optional<Like> userLike = likeRepository.findByMediaAndUser(media, currentUser);
                        String userReaction = userLike.isPresent() ? userLike.get().getType().toString().toLowerCase() : "none";
                        mediaMap.put("userReaction", userReaction);
                    } else {
                        mediaMap.put("userReaction", "none");
                    }
                } catch (Exception e) {
                    System.err.println("Error fetching interaction data for media " + media.getId() + ": " + e.getMessage());
                    mediaMap.put("likeCount", 0);
                    mediaMap.put("dislikeCount", 0);
                    mediaMap.put("commentCount", 0);
                    mediaMap.put("userReaction", "none");
                }

                return mediaMap;
            }).collect(Collectors.toList());

            Map<String, Object> response = new HashMap<>();
            response.put("media", mediaData);
            response.put("count", mediaList.size());
            response.put("status", "SUCCESS");

            System.out.println("Successfully returning " + mediaList.size() + " media items for all users");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("Error in getAllMediaList: " + e.getMessage());
            e.printStackTrace();

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to load all media: " + e.getMessage());
            errorResponse.put("status", "ERROR");
            errorResponse.put("details", e.getClass().getSimpleName());

            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    // Helper method to get authenticated user
    private User getAuthenticatedUser(HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated()) {
                String username = authentication.getName();
                // Try to find user by mobile number (new system) or fallback for legacy
                user = userRepository.findByMobileNumberAndIsVerifiedTrue(username).orElse(null);
            }
        }
        return user;
    }

    // Like/Dislike endpoints
    @PostMapping("/{mediaId}/like")
    @Transactional
    public ResponseEntity<?> likeMedia(@PathVariable Long mediaId, HttpSession session) {
        try {
            User user = getAuthenticatedUser(session);
            if (user == null) {
                return ResponseEntity.status(401).body(Map.of("error", "User not authenticated", "status", "UNAUTHORIZED"));
            }

            Media media = mediaRepository.findById(mediaId)
                    .orElseThrow(() -> new RuntimeException("Media not found"));

            Optional<Like> existingLike = likeRepository.findByMediaAndUser(media, user);

            if (existingLike.isPresent()) {
                Like like = existingLike.get();
                if (like.getType() == Like.LikeType.LIKE) {
                    // User already liked, remove the like
                    likeRepository.delete(like);
                } else {
                    // User disliked before, change to like
                    like.setType(Like.LikeType.LIKE);
                    likeRepository.save(like);
                }
            } else {
                // New like
                Like newLike = new Like(media, user, Like.LikeType.LIKE);
                likeRepository.save(newLike);
            }

            long likeCount = likeRepository.countLikesByMedia(media);
            long dislikeCount = likeRepository.countDislikesByMedia(media);

            Map<String, Object> response = new HashMap<>();
            response.put("status", "SUCCESS");
            response.put("likeCount", likeCount);
            response.put("dislikeCount", dislikeCount);
            response.put("message", "Like updated successfully");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            errorResponse.put("status", "ERROR");
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    @PostMapping("/{mediaId}/dislike")
    @Transactional
    public ResponseEntity<?> dislikeMedia(@PathVariable Long mediaId, HttpSession session) {
        try {
            User user = getAuthenticatedUser(session);
            if (user == null) {
                return ResponseEntity.status(401).body(Map.of("error", "User not authenticated", "status", "UNAUTHORIZED"));
            }

            Media media = mediaRepository.findById(mediaId)
                    .orElseThrow(() -> new RuntimeException("Media not found"));

            Optional<Like> existingLike = likeRepository.findByMediaAndUser(media, user);

            if (existingLike.isPresent()) {
                Like like = existingLike.get();
                if (like.getType() == Like.LikeType.DISLIKE) {
                    // User already disliked, remove the dislike
                    likeRepository.delete(like);
                } else {
                    // User liked before, change to dislike
                    like.setType(Like.LikeType.DISLIKE);
                    likeRepository.save(like);
                }
            } else {
                // New dislike
                Like newDislike = new Like(media, user, Like.LikeType.DISLIKE);
                likeRepository.save(newDislike);
            }

            long likeCount = likeRepository.countLikesByMedia(media);
            long dislikeCount = likeRepository.countDislikesByMedia(media);

            Map<String, Object> response = new HashMap<>();
            response.put("status", "SUCCESS");
            response.put("likeCount", likeCount);
            response.put("dislikeCount", dislikeCount);
            response.put("message", "Dislike updated successfully");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            errorResponse.put("status", "ERROR");
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    // Comment endpoints
    @PostMapping("/{mediaId}/comment")
    public ResponseEntity<?> addComment(@PathVariable Long mediaId,
                                       @RequestBody Map<String, String> commentData,
                                       HttpSession session) {
        try {
            User user = getAuthenticatedUser(session);
            if (user == null) {
                return ResponseEntity.status(401).body(Map.of("error", "User not authenticated", "status", "UNAUTHORIZED"));
            }

            String content = commentData.get("content");
            if (content == null || content.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Comment content is required", "status", "VALIDATION_ERROR"));
            }

            Media media = mediaRepository.findById(mediaId)
                    .orElseThrow(() -> new RuntimeException("Media not found"));

            Comment comment = new Comment(content.trim(), media, user);

            commentRepository.save(comment);

            Map<String, Object> response = new HashMap<>();
            response.put("status", "SUCCESS");
            response.put("message", "Comment added successfully");
            response.put("comment", Map.of(
                "id", comment.getId(),
                "content", comment.getContent(),
                "userName", comment.getUser().getName(),
                "createdAt", comment.getCreatedAt().toString()
            ));

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            errorResponse.put("status", "ERROR");
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    @GetMapping("/{mediaId}/comments")
    public ResponseEntity<?> getComments(@PathVariable Long mediaId) {
        try {
            Media media = mediaRepository.findById(mediaId)
                    .orElseThrow(() -> new RuntimeException("Media not found"));

            List<Comment> comments = commentRepository.findByMediaOrderByCreatedAtDesc(media);

            List<Map<String, Object>> commentData = comments.stream().map(comment -> {
                Map<String, Object> commentMap = new HashMap<>();
                commentMap.put("id", comment.getId());
                commentMap.put("content", comment.getContent());
                commentMap.put("userName", comment.getUser().getName());
                commentMap.put("createdAt", comment.getCreatedAt().toString());
                return commentMap;
            }).collect(Collectors.toList());

            Map<String, Object> response = new HashMap<>();
            response.put("status", "SUCCESS");
            response.put("comments", commentData);
            response.put("count", commentData.size());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            errorResponse.put("status", "ERROR");
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    @GetMapping("/{mediaId}/stats")
    public ResponseEntity<?> getMediaStats(@PathVariable Long mediaId, HttpSession session) {
        try {
            Media media = mediaRepository.findById(mediaId)
                    .orElseThrow(() -> new RuntimeException("Media not found"));

            long likeCount = likeRepository.countLikesByMedia(media);
            long dislikeCount = likeRepository.countDislikesByMedia(media);
            long commentCount = commentRepository.countByMedia(media);

            User currentUser = getAuthenticatedUser(session);
            String userReaction = "none";

            if (currentUser != null) {
                Optional<Like> userLike = likeRepository.findByMediaAndUser(media, currentUser);
                if (userLike.isPresent()) {
                    userReaction = userLike.get().getType().toString().toLowerCase();
                }
            }

            Map<String, Object> response = new HashMap<>();
            response.put("status", "SUCCESS");
            response.put("likeCount", likeCount);
            response.put("dislikeCount", dislikeCount);
            response.put("commentCount", commentCount);
            response.put("userReaction", userReaction);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            errorResponse.put("status", "ERROR");
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    @GetMapping("/debug")
    public ResponseEntity<?> debugMediaEndpoint(HttpSession session) {
        Map<String, Object> debugInfo = new HashMap<>();

        try {
            // Check user authentication
            User sessionUser = (User) session.getAttribute("user");
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            debugInfo.put("sessionUser", sessionUser != null ? sessionUser.getEmail() : "null");
            debugInfo.put("authentication", authentication != null ? authentication.getName() : "null");
            debugInfo.put("isAuthenticated", authentication != null && authentication.isAuthenticated());

            // Check database connectivity
            try {
                long userCount = userRepository.count();
                debugInfo.put("userCount", userCount);
                debugInfo.put("databaseConnected", true);
            } catch (Exception e) {
                debugInfo.put("databaseConnected", false);
                debugInfo.put("databaseError", e.getMessage());
            }

            // Check media count
            try {
                long totalMediaCount = mediaRepository.count();
                debugInfo.put("totalMediaCount", totalMediaCount);
            } catch (Exception e) {
                debugInfo.put("mediaRepositoryError", e.getMessage());
            }

            // Check repositories
            debugInfo.put("mediaService", mediaService != null ? "initialized" : "null");
            debugInfo.put("mediaRepository", mediaRepository != null ? "initialized" : "null");
            debugInfo.put("likeRepository", likeRepository != null ? "initialized" : "null");
            debugInfo.put("commentRepository", commentRepository != null ? "initialized" : "null");

            debugInfo.put("status", "SUCCESS");
            return ResponseEntity.ok(debugInfo);

        } catch (Exception e) {
            debugInfo.put("status", "ERROR");
            debugInfo.put("error", e.getMessage());
            debugInfo.put("errorType", e.getClass().getSimpleName());
            return ResponseEntity.status(500).body(debugInfo);
        }
    }

    @PostMapping("/upload-video")
    public ResponseEntity<?> uploadVideo(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam("title") String title,
            @RequestParam("description") String description,
            @RequestParam("districtId") Long districtId,
            @RequestParam("videoFile") MultipartFile videoFile,
            @RequestParam(value = "thumbnailFile", required = false) MultipartFile thumbnailFile) {

        try {
            // Verify admin authentication
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(401).body(Map.of("error", "Invalid authorization header"));
            }

            String token = authHeader.substring(7);
            if (!isValidAdminToken(token)) {
                return ResponseEntity.status(401).body(Map.of("error", "Unauthorized - Admin access required"));
            }

            // Validate required fields
            if (title == null || title.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Title is required"));
            }

            if (videoFile == null || videoFile.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Video file is required"));
            }

            // Validate video file
            String contentType = videoFile.getContentType();
            if (contentType == null || !contentType.startsWith("video/")) {
                return ResponseEntity.badRequest().body(Map.of("error", "Only video files are allowed"));
            }

            // Check file size (100MB limit for videos)
            long maxSize = 100 * 1024 * 1024; // 100MB
            if (videoFile.getSize() > maxSize) {
                return ResponseEntity.badRequest().body(Map.of("error", "Video file size exceeds 100MB limit"));
            }

            // Validate district
            Optional<District> districtOpt = districtRepository.findById(districtId);
            if (districtOpt.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Invalid district selected"));
            }

            // Upload video file
            String videoPath = mediaService.uploadVideoFile(videoFile, title);

            // Upload thumbnail if provided
            String thumbnailPath = null;
            if (thumbnailFile != null && !thumbnailFile.isEmpty()) {
                String thumbContentType = thumbnailFile.getContentType();
                if (thumbContentType != null && thumbContentType.startsWith("image/")) {
                    thumbnailPath = mediaService.uploadThumbnailFile(thumbnailFile, title);
                }
            }

            // Get video duration (simplified estimation)
            int duration = estimateVideoDuration(videoFile);

            // Create Video entity using the Video repository directly
            Video video = new Video();
            video.setTitle(title.trim());
            video.setDescription(description != null ? description.trim() : "");
            video.setVideoUrl(videoPath);
            video.setThumbnailUrl(thumbnailPath);
            video.setDuration(duration);
            video.setDistrict(districtOpt.get());
            video.setWatchCount(0);
            video.setCreatedAt(java.time.LocalDateTime.now());
            video.setIsActive(true);

            // Get admin info for postedBy field
            String adminUsername = jwtUtil.extractUsername(token);
            if (adminUsername != null && adminUsername.startsWith("admin:")) {
                String realAdminUsername = adminUsername.substring(6);
                Optional<AdminUser> adminOpt = adminUserRepository.findByUsernameAndIsActiveTrue(realAdminUsername);
                if (adminOpt.isPresent()) {
                    video.setPostedBy(adminOpt.get().getId());
                }
            }

            // Save video to database
            Video savedVideo = videoRepository.save(video);

            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Video uploaded successfully",
                "videoUrl", videoPath,
                "thumbnailUrl", thumbnailPath,
                "title", title.trim(),
                "videoId", savedVideo.getId()
            ));

        } catch (Exception e) {
            System.err.println("Video upload error: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", "Upload failed: " + e.getMessage()));
        }
    }

    private boolean isValidAdminToken(String token) {
        // Simple validation - in production, use proper JWT validation
        return token != null && !token.isEmpty();
    }

    private int estimateVideoDuration(MultipartFile videoFile) {
        // Simplified duration estimation - in production, use FFmpeg or similar
        // For now, return a default duration based on file size
        long sizeInMB = videoFile.getSize() / (1024 * 1024);
        return (int) Math.max(30, sizeInMB * 2); // Rough estimate: 2 seconds per MB
    }
}
