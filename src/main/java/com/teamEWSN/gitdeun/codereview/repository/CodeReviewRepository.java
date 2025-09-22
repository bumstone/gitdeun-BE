package com.teamEWSN.gitdeun.codereview.repository;

import com.teamEWSN.gitdeun.codereview.entity.CodeReview;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CodeReviewRepository extends JpaRepository<CodeReview, Long> {
    Page<CodeReview> findByMindmapIdAndNodeKey(Long mindmapId, String nodeKey, Pageable pageable);
}