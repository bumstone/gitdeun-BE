package com.teamEWSN.gitdeun.common.oauth.dto;

import com.teamEWSN.gitdeun.common.oauth.entity.OauthProvider;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class SocialConnectionResponseDto {
    private List<OauthProvider> connectedProviders;
}