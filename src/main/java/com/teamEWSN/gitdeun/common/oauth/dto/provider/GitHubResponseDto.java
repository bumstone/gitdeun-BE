package com.teamEWSN.gitdeun.common.oauth.dto.provider;

import java.util.Map;

public class GitHubResponseDto implements OAuth2ResponseDto {

    private final Map<String, Object> attributes;

    public GitHubResponseDto(Map<String, Object> attributes) {
        this.attributes = attributes;
    }

    @Override
    public String getProvider() {
        return "github";
    }

    @Override
    public String getProviderId() {
        return attributes.get("id").toString();
    }

    @Override
    public String getEmail() {
        return attributes.get("email") != null ? attributes.get("email").toString() : null;
    }

    @Override
    public String getName() {
        return attributes.get("name") != null ? attributes.get("name").toString() : attributes.get("login").toString();
    }

    @Override
    public String getProfileImageUrl() {
        return attributes.get("avatar_url").toString();
    }
}