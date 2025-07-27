package com.teamEWSN.gitdeun.repo.repository;

import com.teamEWSN.gitdeun.repo.entity.Repo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RepoRepository extends JpaRepository<Repo, Long> {

    Optional<Repo> findByGithubRepoUrl(String githubRepoUrl);
}