package com.hivemind.media.entity;

import jakarta.persistence.*;
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
@Entity
@Table(name = "media_files")
public class MediaFile
{
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "media_id")
    private UUID mediaId;

    @Column(name = "uploader_id", nullable = false)
    private UUID uploaderId;

    @Column(name = "original_filename", nullable = false)
    private String originalFilename;

    @Column(name = "s3_key", nullable = false, unique = true)
    private String s3Key;

    @Column(name = "s3_bucket", nullable = false)
    private String s3Bucket;

    @Column(name = "content_type")
    private String contentType;

    @Column(name = "file_size")
    private long fileSize;

    @Column(name = "reference_id")
    private UUID referenceId;

    @Column(name = "reference_type")
    private String referenceType; // POST, GROUP, USER_AVATAR

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
