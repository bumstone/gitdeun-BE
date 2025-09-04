package com.teamEWSN.gitdeun.Application.repository;

import com.teamEWSN.gitdeun.Application.entity.Application;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ApplicationRepository extends JpaRepository<Application, Long> {
}
