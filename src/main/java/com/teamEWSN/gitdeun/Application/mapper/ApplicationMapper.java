package com.teamEWSN.gitdeun.Application.mapper;

import com.teamEWSN.gitdeun.Application.dto.ApplicationListResponseDto;
import com.teamEWSN.gitdeun.Application.dto.ApplicationResponseDto;
import com.teamEWSN.gitdeun.Application.entity.Application;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ApplicationMapper {

    @Mapping(source = "id", target = "applicationId")
    @Mapping(source = "applicant.id", target = "applicantId")
    @Mapping(source = "applicant.name", target = "applicantName")
    @Mapping(source = "applicant.email", target = "applicantEmail")
    @Mapping(source = "applicant.nickname", target = "applicantNickname")
    @Mapping(source = "applicant.profileImage", target = "applicantProfileImage")
    @Mapping(source = "recruitment.id", target = "recruitmentId")
    @Mapping(source = "recruitment.title", target = "recruitmentTitle")
    @Mapping(source = "recruitment.recruiter.name", target = "recruiterName")
    ApplicationResponseDto toResponseDto(Application application);

    @Mapping(source = "id", target = "applicationId")
    @Mapping(source = "applicant.name", target = "applicantName")
    @Mapping(source = "applicant.nickname", target = "applicantNickname")
    @Mapping(source = "applicant.profileImage", target = "applicantProfileImage")
    @Mapping(source = "recruitment.title", target = "recruitmentTitle")
    ApplicationListResponseDto toListResponseDto(Application application);

}
