package com.teamEWSN.gitdeun.user.repository;

import com.teamEWSN.gitdeun.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
}