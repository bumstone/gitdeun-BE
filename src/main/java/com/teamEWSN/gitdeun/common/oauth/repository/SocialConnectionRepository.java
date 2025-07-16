package com.teamEWSN.gitdeun.common.oauth.repository;

import com.teamEWSN.gitdeun.common.oauth.entity.SocialConnection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SocialConnectionRepository extends JpaRepository<SocialConnection, Long> {
}
