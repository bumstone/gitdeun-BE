package com.teamEWSN.gitdeun.repo.mapper;

import com.teamEWSN.gitdeun.repo.dto.RepoResponseDto;
import com.teamEWSN.gitdeun.repo.entity.Repo;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface RepoMapper {

    @Mapping(source = "id",  target = "repoId")
    RepoResponseDto toResponseDto(Repo repo);

}
