package com.hivemind.media.config;

import org.springframework.context.annotation.Configuration;

/**
 * S3 configuration — disabled in local dev mode.
 * The MediaServiceImpl uses local filesystem storage instead.
 * 
 * To enable S3 in production, uncomment the beans below and set
 * AWS_ACCESS_KEY, AWS_SECRET_KEY, AWS_REGION environment variables.
 */
@Configuration
public class S3Config
{
    // S3Client and S3Presigner beans are not needed for local dev.
    // The MediaServiceImpl stores files on the local filesystem.
}
