# Media Service — Code-Level Reference

## MediaServiceApplication

**Package:** `com.hivemind.media`

**Annotations:**
- `@SpringBootApplication` — Enables auto-configuration, component scanning, and configuration properties
- `@EnableDiscoveryClient` — Registers with Eureka service registry

**Design Pattern:** Application Entry Point (Spring Boot convention)

### Methods

#### `main(String[] args)`
- **Signature:** `public static void main(String[] args)`
- **Logic:** `SpringApplication.run(MediaServiceApplication.class, args)`
- **Returns:** void

---

## S3Config

**Package:** `com.hivemind.media.config`

**Annotations:**
- `@Configuration`

**Design Pattern:** Strategy — abstracts storage backend (local filesystem in dev, S3 in production)

### Current State

This is currently a **stub configuration**. In the local development environment, files are stored on the local filesystem. In production, this class would create:

- `S3Client` bean — for upload/download operations
- `S3Presigner` bean — for generating pre-signed URLs

### Future Production Beans

#### `s3Client()` (Not yet implemented)
- **Would create:** `S3Client` configured with region, credentials, and endpoint
- **Purpose:** Direct S3 upload/download operations

#### `s3Presigner()` (Not yet implemented)
- **Would create:** `S3Presigner` for generating time-limited pre-signed download URLs
- **Purpose:** Secure, temporary access to media files without exposing S3 credentials

---

## MediaController

**Package:** `com.hivemind.media.controller`

**Annotations:**
- `@RestController`
- `@RequestMapping("/api/v1/media")`

**Design Pattern:** Façade — exposes simplified REST API over service layer

### Fields (Constructor Injection)

| Field | Type |
|-------|------|
| mediaService | IMediaService |
| mediaFileRepository | MediaFileRepository |

### Endpoints

#### `POST /upload`
- **Signature:** `public ResponseEntity<MediaFileDto> uploadFile(@RequestHeader("X-User-Id") UUID uploaderId, @RequestParam("file") MultipartFile file, @RequestParam(required = false) UUID referenceId, @RequestParam(required = false) String referenceType)`
- **Logic:** Delegates to `mediaService.uploadFile(uploaderId, file, referenceId, referenceType)`
- **Returns:** `201 Created` with `MediaFileDto`
- **Parameters:**
  - `file`: Multipart file upload
  - `referenceId`: Optional — the entity this media relates to (post, group, user)
  - `referenceType`: Optional — type of reference (POST, GROUP, USER_AVATAR)

#### `GET /{mediaId}`
- **Signature:** `public ResponseEntity<MediaFileDto> getMediaById(@PathVariable UUID mediaId)`
- **Logic:** Delegates to `mediaService.getMediaById(mediaId)`
- **Returns:** `MediaFileDto`

#### `GET /{mediaId}/download`
- **Signature:** `public ResponseEntity<Resource> downloadFile(@PathVariable UUID mediaId)`
- **Logic:**
  1. Loads `MediaFile` entity by ID
  2. Constructs file path from `s3Key` field (which holds local path in dev)
  3. Creates `FileSystemResource` pointing to the local file
  4. Returns with appropriate `Content-Type` header and `Content-Disposition: attachment`
- **Returns:** `Resource` (file bytes) with download headers
- **Note:** In production, this would redirect to a pre-signed S3 URL instead

#### `GET /url/{mediaId}`
- **Signature:** `public ResponseEntity<String> getPresignedUrl(@PathVariable UUID mediaId)`
- **Logic:** Delegates to `mediaService.generatePresignedUrl(mediaId)`
- **Returns:** `String` — URL for downloading the file

#### `GET /uploader/{uploaderId}`
- **Signature:** `public ResponseEntity<List<MediaFileDto>> getMediaByUploader(@PathVariable UUID uploaderId)`
- **Logic:** Delegates to `mediaService.getMediaByUploader(uploaderId)`
- **Returns:** `List<MediaFileDto>` — all files uploaded by the user

#### `DELETE /{mediaId}`
- **Signature:** `public ResponseEntity<ApiResponse> deleteMedia(@PathVariable UUID mediaId, @RequestHeader("X-User-Id") UUID userId)`
- **Logic:** Delegates to `mediaService.deleteMedia(mediaId, userId)`
- **Returns:** `ApiResponse` with success message
- **Authorization:** Validates that the requesting user is the uploader

---

## MediaFile (Entity)

**Package:** `com.hivemind.media.entity`

**Annotations:**
- `@Entity` — JPA entity
- `@Table(name = "media_files")` — Maps to PostgreSQL/H2 `media_files` table

**Note:** This service uses JPA/PostgreSQL (not Cassandra) — relational DB is appropriate for file metadata with unique constraints.

### Fields

| Field | Type | Annotation | Description |
|-------|------|------------|-------------|
| mediaId | UUID | `@Id @GeneratedValue(strategy = GenerationType.UUID)` | Auto-generated unique identifier |
| uploaderId | UUID | | User who uploaded the file |
| originalFilename | String | | Original filename from upload |
| s3Key | String | `@Column(unique = true)` | Storage key (local path in dev, S3 key in prod) |
| s3Bucket | String | | Storage bucket ("local" in dev, actual bucket name in prod) |
| contentType | String | | MIME type (e.g., "image/png", "video/mp4") |
| fileSize | Long | | File size in bytes |
| referenceId | UUID | | Referenced entity ID (post, group, user) |
| referenceType | String | | Reference type: "POST", "GROUP", or "USER_AVATAR" |
| createdAt | LocalDateTime | | Upload timestamp |

---

## MediaFileRepository

**Package:** `com.hivemind.media.repository`

**Extends:** `JpaRepository<MediaFile, UUID>`

**Design Pattern:** Repository pattern (Spring Data JPA)

### Methods

#### `findByUploaderId(UUID uploaderId)`
- **Signature:** `List<MediaFile> findByUploaderId(UUID uploaderId)`
- **Logic:** JPA derived query — finds all media files uploaded by a specific user
- **Returns:** `List<MediaFile>`

#### `findByReferenceIdAndReferenceType(UUID referenceId, String referenceType)`
- **Signature:** `List<MediaFile> findByReferenceIdAndReferenceType(UUID referenceId, String referenceType)`
- **Logic:** JPA derived query — finds media files associated with a specific entity
- **Returns:** `List<MediaFile>`
- **Use case:** Find all images for a post, or the avatar for a user

---

## IMediaService (Interface)

**Package:** `com.hivemind.media.service`

### Method Signatures

| Method | Parameters | Returns |
|--------|-----------|---------|
| `uploadFile` | `UUID uploaderId, MultipartFile file, UUID referenceId, String referenceType` | `MediaFileDto` |
| `getMediaById` | `UUID mediaId` | `MediaFileDto` |
| `generatePresignedUrl` | `UUID mediaId` | `String` |
| `getMediaByUploader` | `UUID uploaderId` | `List<MediaFileDto>` |
| `deleteMedia` | `UUID mediaId, UUID userId` | `void` |

---

## MediaServiceImpl

**Package:** `com.hivemind.media.service.impl`

**Annotations:**
- `@Service`

**Implements:** `IMediaService`

**Design Patterns:**
- Service Layer — encapsulates file management business logic
- Strategy — storage implementation varies by environment (local vs S3)

### Fields

| Field | Type | Source |
|-------|------|--------|
| mediaFileRepository | MediaFileRepository | Constructor injection |
| storagePath | String | `@Value("${media.storage.path:./media-uploads}")` — default `./media-uploads` |

### Methods

#### `uploadFile(UUID uploaderId, MultipartFile file, UUID referenceId, String referenceType)`
- **Signature:** `@Override public MediaFileDto uploadFile(UUID uploaderId, MultipartFile file, UUID referenceId, String referenceType)`
- **Logic:**
  1. Creates directory structure: `{storagePath}/{uploaderId}/` (if not exists)
  2. Generates unique filename: `UUID.randomUUID() + "_" + originalFilename` (prevents collisions)
  3. Writes file bytes to local filesystem: `{storagePath}/{uploaderId}/{generatedFilename}`
  4. Builds `MediaFile` entity:
     - `uploaderId` = uploaderId
     - `originalFilename` = file.getOriginalFilename()
     - `s3Key` = local file path (relative)
     - `s3Bucket` = "local"
     - `contentType` = file.getContentType()
     - `fileSize` = file.getSize()
     - `referenceId` = referenceId (nullable)
     - `referenceType` = referenceType (nullable)
     - `createdAt` = LocalDateTime.now()
  5. Saves MediaFile entity via repository
  6. Maps to DTO and returns
- **Returns:** `MediaFileDto`
- **Exceptions:** IOException if file write fails

#### `getMediaById(UUID mediaId)`
- **Signature:** `@Override public MediaFileDto getMediaById(UUID mediaId)`
- **Logic:**
  1. Calls `mediaFileRepository.findById(mediaId)`
  2. If not found → throws RuntimeException ("Media file not found")
  3. Maps to DTO
- **Returns:** `MediaFileDto`
- **Exceptions:** RuntimeException if not found

#### `generatePresignedUrl(UUID mediaId)`
- **Signature:** `@Override public String generatePresignedUrl(UUID mediaId)`
- **Logic:**
  1. Loads MediaFile by ID (throws if not found)
  2. Returns local download URL: `"/api/v1/media/" + mediaId + "/download"`
- **Returns:** `String` — download endpoint URL
- **Note:** In production, this would generate a time-limited S3 pre-signed URL

#### `getMediaByUploader(UUID uploaderId)`
- **Signature:** `@Override public List<MediaFileDto> getMediaByUploader(UUID uploaderId)`
- **Logic:**
  1. Calls `mediaFileRepository.findByUploaderId(uploaderId)`
  2. Maps each entity to DTO
- **Returns:** `List<MediaFileDto>`

#### `deleteMedia(UUID mediaId, UUID userId)`
- **Signature:** `@Override public void deleteMedia(UUID mediaId, UUID userId)`
- **Logic:**
  1. Loads MediaFile by ID (throws if not found)
  2. Validates that `userId` equals `mediaFile.getUploaderId()` — only uploader can delete
  3. If not authorized → throws RuntimeException ("Unauthorized to delete this file")
  4. Deletes the local file from filesystem using the `s3Key` path
  5. Deletes the MediaFile record from database
- **Returns:** void
- **Exceptions:** RuntimeException if not found or unauthorized

---

## DTOs

**Package:** `com.hivemind.media.dto`

### MediaFileDto

| Field | Type | Description |
|-------|------|-------------|
| mediaId | UUID | Unique file identifier |
| uploaderId | UUID | Uploader's user ID |
| originalFilename | String | Original uploaded filename |
| contentType | String | MIME type |
| fileSize | Long | Size in bytes |
| referenceId | UUID | Referenced entity ID |
| referenceType | String | POST, GROUP, or USER_AVATAR |
| createdAt | LocalDateTime | Upload timestamp |

### ApiResponse

| Field | Type | Description |
|-------|------|-------------|
| message | String | Success/error message |
| success | boolean | Operation result |
