package com.teamEWSN.gitdeun.common.s3.controller;

import com.teamEWSN.gitdeun.common.exception.ErrorCode;
import com.teamEWSN.gitdeun.common.exception.GlobalException;
import com.teamEWSN.gitdeun.common.s3.service.S3BucketService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/s3/bucket")
@RequiredArgsConstructor
public class S3BucketController {

    private final S3BucketService s3BucketService;
    private static final int MAX_FILE_COUNT = 10;

    @PostMapping("/upload")
    public ResponseEntity<List<String>> uploadFiles(
        @RequestParam("files") List<MultipartFile> files,
        @RequestParam("path") String path
    ) {
        // FILE-005: 업로드 가능한 파일 개수를 초과했습니다.
        if (files.size() > MAX_FILE_COUNT) {
            throw new GlobalException(ErrorCode.FILE_COUNT_EXCEEDED);
        }

        List<String> fileUrls = s3BucketService.upload(files, path);
        return ResponseEntity.ok(fileUrls);
    }

    @DeleteMapping("/delete")
    public ResponseEntity<Void> deleteFiles(@RequestBody List<String> urls) {
        s3BucketService.remove(urls);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/download")
    public ResponseEntity<Resource> downloadFile(@RequestParam("url") String url) {
        Resource resource = s3BucketService.download(url);
        String filename = URLEncoder.encode(resource.getFilename(), StandardCharsets.UTF_8);

        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
            .body(resource);
    }
}