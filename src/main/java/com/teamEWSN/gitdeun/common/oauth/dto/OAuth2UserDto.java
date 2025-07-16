package com.teamEWSN.gitdeun.common.oauth.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@ToString
@Getter
@Builder
public class OAuth2UserDto {

    private String oauthName;
    private String nickname;
    private String name;
    private String email;
    private String role;
}
