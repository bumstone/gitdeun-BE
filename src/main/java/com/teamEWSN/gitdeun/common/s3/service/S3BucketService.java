package com.teamEWSN.gitdeun.common.s3.service;

import com.teamEWSN.gitdeun.common.exception.ErrorCode;
import com.teamEWSN.gitdeun.common.exception.GlobalException;
import io.awspring.cloud.s3.S3Resource;
import io.awspring.cloud.s3.S3Template;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.exception.SdkException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class S3BucketService {

    private final S3Template s3Template;

    @Value("${cloud.aws.s3.bucket.name}")
    private String bucketName;

    public List<String> upload(List<MultipartFile> files, String path) {
        // FILE-002: 파일 목록이 비어있거나 유효하지 않습니다.
        if (files == null || files.stream().allMatch(MultipartFile::isEmpty)) {
            throw new GlobalException(ErrorCode.INVALID_FILE_LIST);
        }

        List<String> uploadedUrls = new ArrayList<>();
        for (MultipartFile file : files) {
            if (file.isEmpty()) continue;

            if (!isValidFileType(file.getOriginalFilename())) {
                // FILE-004: 지원하지 않는 파일 형식입니다.
                throw new GlobalException(ErrorCode.INVALID_FILE_TYPE);
            }

            String fullPath = generateValidPath(path) + createUniqueFileName(file.getOriginalFilename());

            try {
                S3Resource s3Resource = s3Template.upload(bucketName, fullPath, file.getInputStream());
                uploadedUrls.add(s3Resource.getURL().toString());
            } catch (IOException | SdkException e) {
                // FILE-501: 파일 업로드 중 서버 오류가 발생했습니다.
                throw new GlobalException(ErrorCode.FILE_UPLOAD_FAILED);
            }
        }
        return uploadedUrls;
    }

    public void remove(List<String> urls) {
        // FILE-002: 파일 목록이 비어있거나 유효하지 않습니다.
        if (urls == null || urls.isEmpty()) {
            throw new GlobalException(ErrorCode.INVALID_FILE_LIST);
        }

        for (String url : urls) {
            String key = extractKeyFromUrl(url);

            try {
                if (!s3Template.objectExists(bucketName, key)) {
                    // FILE-001: 요청한 파일을 찾을 수 없습니다.
                    throw new GlobalException(ErrorCode.FILE_NOT_FOUND);
                }
                s3Template.deleteObject(bucketName, key);
            } catch (SdkException e) {
                // FILE-503: 파일 삭제 중 서버 오류가 발생했습니다.
                throw new GlobalException(ErrorCode.FILE_DELETE_FAILED);
            }
        }
    }

    public S3Resource download(String url) {
        String key = extractKeyFromUrl(url);

        try {
            if (!s3Template.objectExists(bucketName, key)) {
                // FILE-001: 요청한 파일을 찾을 수 없습니다.
                throw new GlobalException(ErrorCode.FILE_NOT_FOUND);
            }
            return s3Template.download(bucketName, key);
        } catch (SdkException e) {
            // FILE-502: 파일 다운로드 중 서버 오류가 발생했습니다.
            throw new GlobalException(ErrorCode.FILE_DOWNLOAD_FAILED);
        }
    }

    private String extractKeyFromUrl(String url) {
        try {
            String urlPrefix = "https://" + bucketName + ".s3.";
            int startIndex = url.indexOf(urlPrefix);
            int keyStartIndex = url.indexOf('/', startIndex + urlPrefix.length());
            return url.substring(keyStartIndex + 1);
        } catch (Exception e) {
            // FILE-007: S3 URL 형식이 올바르지 않습니다.
            throw new GlobalException(ErrorCode.INVALID_S3_URL);
        }
    }

    private String generateValidPath(String path) {
        if (path == null || path.trim().isEmpty()) {
            return "";
        }
        if (path.contains("..")) {
            // FILE-003: 파일 경로나 이름이 유효하지 않습니다.
            throw new GlobalException(ErrorCode.INVALID_FILE_PATH);
        }
        return path.replaceAll("^/+|/+$", "") + "/";
    }

    private String createUniqueFileName(String originalFileName) {
        String extension = StringUtils.getFilenameExtension(originalFileName);
        return UUID.randomUUID() + "." + extension;
    }

    private boolean isValidFileType(String filename) {
        if (filename == null) return false;
        String extension = StringUtils.getFilenameExtension(filename.toLowerCase());
        List<String> allowedExtensions = List.of("jpg", "jpeg", "png", "gif", "pdf", "docs"); // 허용 확장자
        return allowedExtensions.contains(extension);
    }
}