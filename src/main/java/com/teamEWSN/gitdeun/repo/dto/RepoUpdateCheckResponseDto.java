package com.teamEWSN.gitdeun.repo.dto;


import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class RepoUpdateCheckResponseDto {
    private final boolean updateNeeded;
}
