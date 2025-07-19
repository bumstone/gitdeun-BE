package com.teamEWSN.gitdeun.common.oauth.dto.provider;

import java.util.Map;

public class GoogleResponseDto implements OAuth2ResponseDto {
    private final Map<String, Object> attributes;

    public GoogleResponseDto(Map<String, Object> attributes) {
        this.attributes = attributes;
    }

    @Override
    public String getProvider() {
        return "google";
    }

    @Override
    public String getProviderId() {
        return attributes.get("sub").toString();
    }

    @Override
    public String getEmail() {
        return attributes.get("email").toString();
    }

    @Override
    public String getName() {
        return attributes.get("name").toString();
    }

    @Override
    public String getNickname() {
        // 별도 닉네임이 없으므로 이름(name)을 그대로 반환
        return attributes.get("name").toString();
    }

    @Override
    public String getProfileImageUrl() {
        return attributes.get("picture").toString();
    }
}