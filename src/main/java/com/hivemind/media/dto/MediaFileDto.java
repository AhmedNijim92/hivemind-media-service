package com.hivemind.media.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MediaFileDto
{
    private UUID mediaId;
    private UUID uploaderId;
    private String originalFilename;
    private String contentType;
    private long fileSize;
    private UUID referenceId;
    private String referenceType;
    private LocalDateTime createdAt;
}
