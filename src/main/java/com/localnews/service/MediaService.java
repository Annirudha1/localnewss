package com.localnews.service;

import com.localnews.entity.Media;
import com.localnews.entity.User;
import com.localnews.repository.MediaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
public class MediaService {

    @Value("${upload.dir}")
    private String uploadDir;

    @Value("${upload.allowed-types}")
    private String[] allowedTypes;

    @Value("${upload.max-files:10}")
    private int maxFiles;

    @Value("${aws.s3.enabled:false}")
    private boolean s3Enabled;

    @Autowired
    private MediaRepository mediaRepository;

    @Autowired(required = false)
    private S3FileStorageService s3FileStorageService;

    public String uploadFile(MultipartFile file, User user, String title) throws IOException {
        try {
            System.out.println("Starting file upload for user: " + user.getEmail());
            System.out.println("Upload storage type: " + (s3Enabled ? "AWS S3" : "Local filesystem"));
            System.out.println("File details - Name: " + file.getOriginalFilename() + ", Size: " + file.getSize() + " bytes, Title: " + title);

            // Validate file type
            validateFileType(file);

            // Use S3 or local storage based on configuration
            String filePath;
            String thumbnailPath = null;

            if (s3Enabled && s3FileStorageService != null) {
                // Upload to S3
                filePath = s3FileStorageService.uploadFile(file, "media");
                // For S3, we'll generate thumbnails later or use a different approach
                System.out.println("File uploaded to S3: " + filePath);
            } else {
                // Use local storage
                filePath = uploadToLocalStorage(file);
                thumbnailPath = generateLocalThumbnail(filePath, file.getContentType());
                System.out.println("File uploaded locally: " + filePath);
            }

            // Save file info to database
            saveMediaRecord(file, user, title, filePath, thumbnailPath);

            return filePath;

        } catch (IOException e) {
            System.err.println("File upload failed: " + e.getMessage());
            e.printStackTrace();
            throw e;
        } catch (Exception e) {
            System.err.println("Unexpected error during file upload: " + e.getMessage());
            e.printStackTrace();
            throw new IOException("File upload failed: " + e.getMessage(), e);
        }
    }

    private void validateFileType(MultipartFile file) throws IOException {
        String contentType = file.getContentType();
        if (contentType == null || !Arrays.asList(allowedTypes).contains(contentType)) {
            throw new IOException("File type not allowed. Allowed types: " + String.join(", ", allowedTypes));
        }
    }

    private String uploadToLocalStorage(MultipartFile file) throws IOException {
        // Create uploads directory if it doesn't exist
        Path uploadPath = Paths.get(uploadDir);
        Path thumbnailPath = Paths.get(uploadDir, "thumbnails");

        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }
        if (!Files.exists(thumbnailPath)) {
            Files.createDirectories(thumbnailPath);
        }

        // Generate unique filename
        String originalFileName = file.getOriginalFilename();
        if (originalFileName == null || originalFileName.trim().isEmpty()) {
            originalFileName = "unnamed_file";
        }

        String fileExtension = "";
        if (originalFileName.contains(".")) {
            fileExtension = originalFileName.substring(originalFileName.lastIndexOf("."));
        }
        String uniqueFileName = UUID.randomUUID().toString() + fileExtension;

        // Save file
        Path filePath = uploadPath.resolve(uniqueFileName);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        // Verify file was created
        if (!Files.exists(filePath)) {
            throw new IOException("File was not created on disk: " + filePath);
        }

        return filePath.toString();
    }

    private String generateLocalThumbnail(String originalFilePath, String contentType) {
        try {
            Path originalPath = Paths.get(originalFilePath);
            Path thumbnailDir = Paths.get(uploadDir, "thumbnails");
            String fileName = originalPath.getFileName().toString();
            String thumbnailFileName = "thumb_" + fileName;
            Path thumbnailPath = thumbnailDir.resolve(thumbnailFileName);

            if (contentType != null && contentType.startsWith("image/")) {
                BufferedImage originalImage = ImageIO.read(originalPath.toFile());
                if (originalImage != null) {
                    BufferedImage thumbnail = createImageThumbnail(originalImage, 200, 200);
                    String format = getImageFormat(fileName);
                    ImageIO.write(thumbnail, format, thumbnailPath.toFile());
                    return thumbnailPath.toString();
                }
            } else if (contentType != null && contentType.startsWith("video/")) {
                BufferedImage videoThumbnail = createVideoThumbnail(200, 200);
                ImageIO.write(videoThumbnail, "png", thumbnailPath.toFile());
                return thumbnailPath.toString();
            }
        } catch (Exception e) {
            System.err.println("Failed to generate thumbnail: " + e.getMessage());
        }
        return null;
    }

    private void saveMediaRecord(MultipartFile file, User user, String title, String filePath, String thumbnailPath) {
        try {
            Media media = new Media();
            media.setTitle(title != null && !title.trim().isEmpty() ? title.trim() : file.getOriginalFilename());
            media.setFilename(file.getOriginalFilename());
            media.setFilePath(filePath);
            media.setThumbnailPath(thumbnailPath);
            media.setFileType(file.getContentType());
            media.setFileSize(file.getSize());
            media.setUser(user);
            media.setUploadedAt(LocalDateTime.now());

            Media savedMedia = mediaRepository.save(media);
            System.out.println("Media record saved to database with ID: " + savedMedia.getId());

        } catch (Exception dbException) {
            System.err.println("Database save failed, but file was uploaded to: " + filePath);
            System.err.println("Database error: " + dbException.getMessage());
            dbException.printStackTrace();
        }
    }

    private BufferedImage createImageThumbnail(BufferedImage original, int width, int height) {
        BufferedImage thumbnail = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = thumbnail.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.drawImage(original, 0, 0, width, height, null);
        g2d.dispose();
        return thumbnail;
    }

    private BufferedImage createVideoThumbnail(int width, int height) {
        BufferedImage thumbnail = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = thumbnail.createGraphics();

        // Create video icon background
        g2d.setColor(Color.DARK_GRAY);
        g2d.fillRect(0, 0, width, height);

        // Draw play button
        g2d.setColor(Color.WHITE);
        int playSize = Math.min(width, height) / 3;
        int playX = (width - playSize) / 2;
        int playY = (height - playSize) / 2;

        int[] xPoints = {playX, playX, playX + playSize};
        int[] yPoints = {playY, playY + playSize, playY + playSize/2};
        g2d.fillPolygon(xPoints, yPoints, 3);

        g2d.dispose();
        return thumbnail;
    }

    private String getImageFormat(String fileName) {
        String extension = fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
        switch (extension) {
            case "png": return "png";
            case "gif": return "gif";
            case "bmp": return "bmp";
            default: return "jpg";
        }
    }

    public String uploadVideoFile(MultipartFile videoFile, String title) throws IOException {
        try {
            System.out.println("Starting video file upload - Title: " + title);
            System.out.println("Video file details - Name: " + videoFile.getOriginalFilename() + ", Size: " + videoFile.getSize() + " bytes");

            // Create upload directory if it doesn't exist
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            // Generate unique filename
            String originalFilename = videoFile.getOriginalFilename();
            String fileExtension = originalFilename != null ?
                originalFilename.substring(originalFilename.lastIndexOf(".")) : ".mp4";
            String uniqueFilename = "video_" + UUID.randomUUID().toString() + fileExtension;

            // Save file
            Path filePath = uploadPath.resolve(uniqueFilename);
            Files.copy(videoFile.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            // Return the URL path for accessing the video
            String videoUrl = "/uploads/" + uniqueFilename;
            System.out.println("Video uploaded successfully to: " + videoUrl);

            return videoUrl;

        } catch (Exception e) {
            System.err.println("Error uploading video file: " + e.getMessage());
            throw new IOException("Failed to upload video file: " + e.getMessage(), e);
        }
    }

    public String uploadThumbnailFile(MultipartFile thumbnailFile, String title) throws IOException {
        try {
            System.out.println("Starting thumbnail upload for: " + title);
            System.out.println("Thumbnail file details - Name: " + thumbnailFile.getOriginalFilename() + ", Size: " + thumbnailFile.getSize() + " bytes");

            // Create upload directory if it doesn't exist
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            // Generate unique filename
            String originalFilename = thumbnailFile.getOriginalFilename();
            String fileExtension = originalFilename != null ?
                originalFilename.substring(originalFilename.lastIndexOf(".")) : ".jpg";
            String uniqueFilename = "thumb_" + UUID.randomUUID().toString() + fileExtension;

            // Save file
            Path filePath = uploadPath.resolve(uniqueFilename);
            Files.copy(thumbnailFile.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            // Return the URL path for accessing the thumbnail
            String thumbnailUrl = "/uploads/" + uniqueFilename;
            System.out.println("Thumbnail uploaded successfully to: " + thumbnailUrl);

            return thumbnailUrl;

        } catch (Exception e) {
            System.err.println("Error uploading thumbnail file: " + e.getMessage());
            throw new IOException("Failed to upload thumbnail file: " + e.getMessage(), e);
        }
    }

    public List<Media> getAllMedia() {
        return mediaRepository.findAll();
    }

    public List<Media> getAllMediaByUser(User user) {
        return mediaRepository.findByUserOrderByUploadedAtDesc(user);
    }
}
