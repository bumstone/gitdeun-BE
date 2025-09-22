package com.teamEWSN.gitdeun.comment.service;

import com.teamEWSN.gitdeun.comment.entity.AttachmentType;
import com.teamEWSN.gitdeun.comment.entity.Comment;
import com.teamEWSN.gitdeun.comment.entity.CommentAttachment;
import com.teamEWSN.gitdeun.comment.repository.CommentAttachmentRepository;
import com.teamEWSN.gitdeun.common.s3.service.S3BucketService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
public class CommentAttachmentService {

    private final S3BucketService s3BucketService;
    private final CommentAttachmentRepository attachmentRepository;

    @Transactional
    public List<CommentAttachment> saveAttachments(Comment comment, List<MultipartFile> files) {
        if (CollectionUtils.isEmpty(files)) {
            return Collections.emptyList();
        }

        String s3Path = String.format("comments/%d/attachments", comment.getId());
        List<String> uploadedUrls = s3BucketService.upload(files, s3Path);

        List<CommentAttachment> attachments = IntStream.range(0, files.size())
            .mapToObj(i -> {
                MultipartFile file = files.get(i);
                String url = uploadedUrls.get(i);
                return CommentAttachment.builder()
                    .comment(comment)
                    .url(url)
                    .fileName(file.getOriginalFilename())
                    .mimeType(file.getContentType())
                    .size(file.getSize())
                    .attachmentType(determineAttachmentType(file.getContentType()))
                    .build();
            })
            .collect(Collectors.toList());

        return attachmentRepository.saveAll(attachments);
    }

    private AttachmentType determineAttachmentType(String mimeType) {
        if (mimeType != null && mimeType.startsWith("image/")) {
            return AttachmentType.IMAGE;
        }
        return AttachmentType.FILE;
    }

    /**
     * 첨부파일 목록을 S3와 DB에서 모두 삭제 (Hard Delete)
     */
    @Transactional
    public void deleteAttachments(List<CommentAttachment> attachments) {
        if (CollectionUtils.isEmpty(attachments)) {
            return;
        }

        // 1. S3에서 파일 삭제
        List<String> urlsToDelete = attachments.stream()
            .map(CommentAttachment::getUrl)
            .collect(Collectors.toList());
        s3BucketService.remove(urlsToDelete);

        // 2. DB에서 첨부파일 정보 삭제
        attachmentRepository.deleteAll(attachments);
    }

}
