package com.hivemind.media.service;

import com.hivemind.media.dto.MediaFileDto;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

public interface IMediaService
{
    MediaFileDto uploadFile(UUID uploaderId, MultipartFile file, UUID referenceId, String referenceType);

    MediaFileDto getMediaById(UUID mediaId);

    String generatePresignedUrl(UUID mediaId);

    List<MediaFileDto> getMediaByUploader(UUID uploaderId);

    void deleteMedia(UUID mediaId, UUID requesterId);
}
