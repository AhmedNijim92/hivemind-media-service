package com.hivemind.media.repository;

import com.hivemind.media.entity.MediaFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MediaFileRepository extends JpaRepository<MediaFile, UUID>
{
    List<MediaFile> findByUploaderId(UUID uploaderId);

    List<MediaFile> findByReferenceIdAndReferenceType(UUID referenceId, String referenceType);
}
