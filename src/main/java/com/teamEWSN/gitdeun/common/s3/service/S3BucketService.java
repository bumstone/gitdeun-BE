package com.teamEWSN.gitdeun.common.s3.service;

import com.teamEWSN.gitdeun.common.exception.ErrorCode;
import com.teamEWSN.gitdeun.common.exception.GlobalException;
import io.awspring.cloud.s3.S3Resource;
import io.awspring.cloud.s3.S3Template;
import lombok.RequiredArgsConstructor;
import org.apache.tika.Tika;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.exception.SdkException;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class S3BucketService {

    private final S3Template s3Template;
    private final Tika tika = new Tika();

    @Value("${s3.bucket}")
    private String bucketName;

    // 이미지 및 파일 업로드 제약 조건
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB
    private static final int MAX_FILE_COUNT = 5;
    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of("image/jpeg", "image/png", "image/gif");
    private static final Set<String> ALLOWED_DOCUMENT_TYPES = Set.of("application/pdf", "application/msword", "application/vnd.openxmlformats-officedocument.wordprocessingml.document");

    public List<String> upload(List<MultipartFile> files, String path) {
        // 1. 파일 목록이 비어있는지 확인
        if (files == null || files.stream().allMatch(MultipartFile::isEmpty)) {
            throw new GlobalException(ErrorCode.INVALID_FILE_LIST);
        }

        // 2. 파일 개수 제한 확인
        if (files.size() > MAX_FILE_COUNT) {
            throw new GlobalException(ErrorCode.FILE_COUNT_EXCEEDED);
        }

        List<String> uploadedUrls = new ArrayList<>();
        for (MultipartFile file : files) {
            try (InputStream inputStream = file.getInputStream()) {
                // 3. 각 파일에 대해 크기 및 실제 타입(매직 바이트) 검증
                validateSingleFile(file.getSize(), inputStream);

                String fullPath = generateValidPath(path) + createUniqueFileName(file.getOriginalFilename());

                // 4. S3에 업로드 (주의: 검증 시 사용한 inputStream을 재사용할 수 없으므로, S3Template이 내부적으로 새 스트림을 열도록 file을 전달)
                S3Resource s3Resource = s3Template.upload(bucketName, fullPath, file.getInputStream());
                uploadedUrls.add(s3Resource.getURL().toString());

            } catch (IOException | SdkException e) {
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
            URI uri = new URI(url);
            String path = uri.getPath();
            // 맨 앞의 '/'를 제거하여 순수한 객체 키만 반환
            return path.substring(1);
        } catch (Exception e) {
            // FILE-007: S3 URL 형식이 올바르지 않습니다.
            throw new GlobalException(ErrorCode.INVALID_S3_URL);
        }
    }

    private String generateValidPath(String path) {
        if (path == null || path.trim().isEmpty()) {
            return "";
        }
        // 경로에 허용된 문자(영문, 숫자, 슬래시, 하이픈, 언더스코어) 외에 다른 문자가 있는지 확인
        if (!path.matches("^[a-zA-Z0-9/_-]+$")) {
            // FILE-003: 파일 경로나 이름이 유효하지 않습니다.
            throw new GlobalException(ErrorCode.INVALID_FILE_PATH);
        }
        // ".." 문자열 체크는 유지
        if (path.contains("..")) {
            throw new GlobalException(ErrorCode.INVALID_FILE_PATH);
        }
        return path.replaceAll("^/+|/+$", "") + "/";
    }

    private String createUniqueFileName(String originalFileName) {
        String extension = StringUtils.getFilenameExtension(originalFileName);
        return UUID.randomUUID() + "." + extension;
    }

    private void validateSingleFile(long fileSize, InputStream inputStream) {
        // 파일 크기 검증
        if (fileSize > MAX_FILE_SIZE) {
            throw new GlobalException(ErrorCode.FILE_SIZE_EXCEEDED);
        }

        try {
            // Tika를 사용하여 InputStream에서 실제 MIME 타입 감지
            String actualMimeType = tika.detect(inputStream);

            // 허용된 이미지 또는 문서 타입이 아니면 예외 발생
            if (actualMimeType == null || (!ALLOWED_IMAGE_TYPES.contains(actualMimeType) && !ALLOWED_DOCUMENT_TYPES.contains(actualMimeType))) {
                throw new GlobalException(ErrorCode.INVALID_FILE_TYPE);
            }
        } catch (IOException e) {
            // 스트림 읽기 중 오류 발생 시
            throw new GlobalException(ErrorCode.FILE_UPLOAD_FAILED);
        }
    }

    /*private void validateFiles(List<MultipartFile> files) {
        if (files.size() > MAX_FILE_COUNT) {
            throw new GlobalException(ErrorCode.FILE_COUNT_EXCEEDED);
        }

        for (MultipartFile file : files) {
            if (file.getSize() > MAX_FILE_SIZE) {
                throw new GlobalException(ErrorCode.FILE_SIZE_EXCEEDED);
            }

            String contentType = file.getContentType();
            // 이미지 또는 문서 타입에 해당하지 않으면 예외 발생
            if (contentType == null || (!ALLOWED_IMAGE_TYPES.contains(contentType) && !ALLOWED_DOCUMENT_TYPES.contains(contentType))) {
                throw new GlobalException(ErrorCode.INVALID_FILE_TYPE);
            }
        }
    }*/
}