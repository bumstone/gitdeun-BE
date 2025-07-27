package com.teamEWSN.gitdeun.mindmap.repository;

import com.teamEWSN.gitdeun.mindmap.entity.Mindmap;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MindmapRepository extends JpaRepository<Mindmap, Integer> {
    
}