package com.teamEWSN.gitdeun.user.repository;

import com.teamEWSN.gitdeun.user.entity.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // user id로 검색
    Optional<User> findByIdAndDeletedAtIsNull(Long id);

    // user email로 검색
    Optional<User> findByEmailAndDeletedAtIsNull(String email);

    // 신규 생성/백필 시 충돌 검사
    boolean existsByHandle(String handle);
    // 수정·보정 시 “본인 제외” 충돌 검사
    boolean existsByHandleAndIdNot(String handle, Long id);
}