package com.hivemind.media.service.impl;

import com.hivemind.media.dto.MediaFileDto;
import com.hivemind.media.entity.MediaFile;
import com.hivemind.media.repository.MediaFileRepository;
import com.hivemind.media.service.IMediaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Media service that stores files on the local filesystem in dev mode.
 * In production, swap this for the S3-based implementation.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MediaServiceImpl implements IMediaService
{
    private final MediaFileRepository mediaFileRepository;

    @Value("${media.storage.path:./media-uploads}")
    private String storagePath;

    @Override
    public MediaFileDto uploadFile(UUID uploaderId, MultipartFile file, UUID referenceId, String referenceType)
    {
        try
        {
            // Create storage directory if it doesn't exist
            Path uploadDir = Paths.get(storagePath, uploaderId.toString());
            Files.createDirectories(uploadDir);

            // Generate unique filename
            String filename = UUID.randomUUID() + "_" + file.getOriginalFilename();
            Path filePath = uploadDir.resolve(filename);

            // Save file to local filesystem
            Files.write(filePath, file.getBytes());

            // Save metadata to PostgreSQL
            MediaFile mediaFile = MediaFile.builder()
                    .uploaderId(uploaderId)
                    .originalFilename(file.getOriginalFilename())
                    .s3Key(filePath.toString())          // store local path as "s3Key"
                    .s3Bucket("local")                    // marker for local storage
                    .contentType(file.getContentType())
                    .fileSize(file.getSize())
                    .referenceId(referenceId)
                    .referenceType(referenceType)
                    .createdAt(LocalDateTime.now())
                    .build();

            MediaFile saved = mediaFileRepository.save(mediaFile);
            log.info("File uploaded locally: {} by user: {}", saved.getMediaId(), uploaderId);
            return toDto(saved);
        }
        catch (IOException e)
        {
            throw new RuntimeException("Failed to store file locally: " + e.getMessage(), e);
        }
    }

    @Override
    public MediaFileDto getMediaById(UUID mediaId)
    {
        MediaFile media = mediaFileRepository.findById(mediaId)
                .orElseThrow(() -> new RuntimeException("Media not found: " + mediaId));
        return toDto(media);
    }

    @Override
    public String generatePresignedUrl(UUID mediaId)
    {
        MediaFile media = mediaFileRepository.findById(mediaId)
                .orElseThrow(() -> new RuntimeException("Media not found: " + mediaId));

        // In local mode, return a file:// URL or a relative path
        // In production, this would generate an S3 presigned URL
        return "/api/v1/media/" + mediaId + "/download";
    }

    @Override
    public List<MediaFileDto> getMediaByUploader(UUID uploaderId)
    {
        return mediaFileRepository.findByUploaderId(uploaderId).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteMedia(UUID mediaId, UUID requesterId)
    {
        MediaFile media = mediaFileRepository.findById(mediaId)
                .orElseThrow(() -> new RuntimeException("Media not found: " + mediaId));

        if (!media.getUploaderId().equals(requesterId))
        {
            throw new RuntimeException("Unauthorized to delete this media");
        }

        // Delete local file
        try
        {
            Files.deleteIfExists(Paths.get(media.getS3Key()));
        }
        catch (IOException e)
        {
            log.warn("Could not delete local file: {}", e.getMessage());
        }

        mediaFileRepository.delete(media);
        log.info("Media {} deleted by user {}", mediaId, requesterId);
    }

    private MediaFileDto toDto(MediaFile media)
    {
        return MediaFileDto.builder()
                .mediaId(media.getMediaId())
                .uploaderId(media.getUploaderId())
                .originalFilename(media.getOriginalFilename())
                .contentType(media.getContentType())
                .fileSize(media.getFileSize())
                .referenceId(media.getReferenceId())
                .referenceType(media.getReferenceType())
                .createdAt(media.getCreatedAt())
                .build();
    }
}
