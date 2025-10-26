package com.localnews.service;

import com.localnews.dto.VideoDto;
import com.localnews.entity.*;
import com.localnews.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class VideoService {

    @Autowired
    private VideoRepository videoRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserVideoInteractionRepository interactionRepository;

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private UserDeviceRepository userDeviceRepository;

    @Autowired
    private SmsService smsService;

    public Page<VideoDto> getVideoFeed(User user, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);

        // Get videos in LRU order (most recent first)
        Page<Video> videos;

        if (user.getDistrict() != null) {
            // Show videos from user's district first, then others
            videos = videoRepository.findByDistrictIdAndIsActiveTrueOrderByCreatedAtDesc(
                user.getDistrict().getId(), pageable);
        } else {
            videos = videoRepository.findAllActiveVideosOrderByCreatedAtDesc(pageable);
        }

        return videos.map(video -> convertToVideoDto(video, user));
    }

    public Optional<VideoDto> getVideoById(Long videoId, User user) {
        Optional<Video> videoOpt = videoRepository.findById(videoId);

        if (videoOpt.isPresent() && videoOpt.get().getIsActive()) {
            return Optional.of(convertToVideoDto(videoOpt.get(), user));
        }

        return Optional.empty();
    }

    @Transactional
    public boolean recordWatch(Long videoId, User user, Integer watchDuration) {
        try {
            Optional<Video> videoOpt = videoRepository.findById(videoId);
            if (videoOpt.isEmpty()) {
                return false;
            }

            Video video = videoOpt.get();

            // Find or create interaction record
            UserVideoInteraction interaction = interactionRepository.findByUserAndVideo(user, video)
                .orElse(new UserVideoInteraction(user, video));

            // Update watch status and duration
            if (!interaction.getHasWatched()) {
                interaction.setHasWatched(true);
                // Increment video watch count
                video.setWatchCount(video.getWatchCount() + 1);
                videoRepository.save(video);
            }

            // Update watch duration (keep the maximum duration watched)
            if (watchDuration > interaction.getWatchDuration()) {
                interaction.setWatchDuration(watchDuration);
            }

            interactionRepository.save(interaction);
            return true;

        } catch (Exception e) {
            return false;
        }
    }

    @Transactional
    public VideoDto toggleLike(Long videoId, User user) {
        Optional<Video> videoOpt = videoRepository.findById(videoId);
        if (videoOpt.isEmpty()) {
            return null;
        }

        Video video = videoOpt.get();

        // Find or create interaction record
        UserVideoInteraction interaction = interactionRepository.findByUserAndVideo(user, video)
            .orElse(new UserVideoInteraction(user, video));

        boolean wasLiked = interaction.getHasLiked();
        interaction.setHasLiked(!wasLiked);

        // Update video like count
        if (wasLiked) {
            video.setLikeCount(Math.max(0, video.getLikeCount() - 1));
        } else {
            video.setLikeCount(video.getLikeCount() + 1);
        }

        interactionRepository.save(interaction);
        videoRepository.save(video);

        return convertToVideoDto(video, user);
    }

    private VideoDto convertToVideoDto(Video video, User user) {
        VideoDto dto = new VideoDto();
        dto.setId(video.getId());
        dto.setTitle(video.getTitle());
        dto.setDescription(video.getDescription());
        dto.setVideoUrl(video.getVideoUrl());
        dto.setThumbnailUrl(video.getThumbnailUrl());
        dto.setDuration(video.getDuration());
        dto.setLikeCount(video.getLikeCount());
        dto.setWatchCount(video.getWatchCount());
        dto.setCreatedAt(video.getCreatedAt());

        if (video.getDistrict() != null) {
            dto.setDistrictName(video.getDistrict().getName());
            dto.setDistrictId(video.getDistrict().getId());
        }

        // Get user interaction data
        if (user != null) {
            Optional<UserVideoInteraction> interaction =
                interactionRepository.findByUserAndVideo(user, video);

            if (interaction.isPresent()) {
                dto.setHasLiked(interaction.get().getHasLiked());
                dto.setHasWatched(interaction.get().getHasWatched());
            }
        }

        // Get comment count (only visible comments)
        Long commentCount = commentRepository.countByVideoAndIsVisibleToUserTrue(video);
        dto.setCommentCount(commentCount.intValue());

        return dto;
    }

    @Transactional
    public void notifyNewVideo(Video video) {
        try {
            // Get all active devices for users in the same district
            List<User> districtUsers = video.getDistrict() != null ?
                userRepository.findByDistrictAndIsVerifiedTrue(video.getDistrict()) :
                userRepository.findAll().stream()
                    .filter(User::getIsVerified)
                    .collect(Collectors.toList());

            for (User user : districtUsers) {
                List<UserDevice> devices = userDeviceRepository.findActiveDevicesByUser(user);

                for (UserDevice device : devices) {
                    smsService.sendPushNotification(
                        device.getDeviceToken(),
                        "New Video Available!",
                        video.getTitle()
                    );
                }
            }

        } catch (Exception e) {
            // Log error but don't fail the video creation
            System.err.println("Error sending push notifications: " + e.getMessage());
        }
    }
}
