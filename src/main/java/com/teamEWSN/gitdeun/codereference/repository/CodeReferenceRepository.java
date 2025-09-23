package com.teamEWSN.gitdeun.codereference.repository;

import com.teamEWSN.gitdeun.codereference.entity.CodeReference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CodeReferenceRepository extends JpaRepository<CodeReference, Long> {

    List<CodeReference> findByMindmapIdAndNodeKey(Long mindmapId, String nodeKey);

    Optional<CodeReference> findByMindmapIdAndId(Long mindmapId, Long id);

    boolean existsByMindmapIdAndId(Long mindmapId, Long id);
}