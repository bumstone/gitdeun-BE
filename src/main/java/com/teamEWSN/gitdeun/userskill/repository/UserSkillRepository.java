package com.teamEWSN.gitdeun.userskill.repository;

import com.teamEWSN.gitdeun.userskill.entity.UserSkill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserSkillRepository extends JpaRepository<UserSkill, Long> {

    // 사용자 ID에 해당하는 모든 기술 항목을 조회
    List<UserSkill> findByUserId(Long userId);

    // 사용자 ID로 기술 삭제
    @Modifying
    @Query("DELETE FROM UserSkill us WHERE us.user.id = :userId")
    void deleteByUserId(@Param("userId") Long userId);
}
