package com.hivemind.media.controller;

import com.hivemind.common.dto.ApiResponse;
import com.hivemind.media.dto.MediaFileDto;
import com.hivemind.media.entity.MediaFile;
import com.hivemind.media.repository.MediaFileRepository;
import com.hivemind.media.service.IMediaService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/media")
@RequiredArgsConstructor
public class MediaController
{
    private final IMediaService mediaService;
    private final MediaFileRepository mediaFileRepository;

    @PostMapping("/upload")
    public ResponseEntity<MediaFileDto> uploadFile(
            @RequestHeader("X-User-Id") UUID userId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "referenceId", required = false) UUID referenceId,
            @RequestParam(value = "referenceType", defaultValue = "POST") String referenceType)
    {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(mediaService.uploadFile(userId, file, referenceId, referenceType));
    }

    @GetMapping("/{mediaId}")
    public ResponseEntity<MediaFileDto> getMediaById(@PathVariable UUID mediaId)
    {
        return ResponseEntity.ok(mediaService.getMediaById(mediaId));
    }

    @GetMapping("/{mediaId}/presigned-url")
    public ResponseEntity<String> getPresignedUrl(@PathVariable UUID mediaId)
    {
        return ResponseEntity.ok(mediaService.generatePresignedUrl(mediaId));
    }

    /**
     * Serve the actual file bytes — used in local dev mode.
     * In production, the presigned-url endpoint returns an S3 URL instead.
     */
    @GetMapping("/{mediaId}/download")
    public ResponseEntity<Resource> downloadFile(@PathVariable UUID mediaId)
    {
        MediaFile media = mediaFileRepository.findById(mediaId)
                .orElseThrow(() -> new RuntimeException("Media not found: " + mediaId));

        Path filePath = Paths.get(media.getS3Key());
        Resource resource = new FileSystemResource(filePath);

        if (!resource.exists())
        {
            return ResponseEntity.notFound().build();
        }

        String contentType = media.getContentType() != null ? media.getContentType() : "application/octet-stream";

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + media.getOriginalFilename() + "\"")
                .body(resource);
    }

    @GetMapping("/my")
    public ResponseEntity<List<MediaFileDto>> getMyMedia(@RequestHeader("X-User-Id") UUID userId)
    {
        return ResponseEntity.ok(mediaService.getMediaByUploader(userId));
    }

    @DeleteMapping("/{mediaId}")
    public ResponseEntity<ApiResponse> deleteMedia(
            @PathVariable UUID mediaId,
            @RequestHeader("X-User-Id") UUID userId)
    {
        mediaService.deleteMedia(mediaId, userId);
        return ResponseEntity.ok(new ApiResponse("Media deleted successfully"));
    }
}
