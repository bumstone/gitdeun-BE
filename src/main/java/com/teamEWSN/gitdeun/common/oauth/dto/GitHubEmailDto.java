package com.teamEWSN.gitdeun.common.oauth.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GitHubEmailDto {

    private String email;
    private boolean primary;
    private boolean verified;
    private String visibility;
}
