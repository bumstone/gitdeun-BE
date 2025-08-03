package com.teamEWSN.gitdeun.user.repository;

import com.teamEWSN.gitdeun.user.entity.UserSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserSettingRepository extends JpaRepository<UserSetting, Long> {

    // 사용자 ID로 설정 조회
    Optional<UserSetting> findByUserId(Long userId);
}