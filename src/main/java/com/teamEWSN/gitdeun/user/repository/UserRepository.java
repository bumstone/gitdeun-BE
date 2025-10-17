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
}