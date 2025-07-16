package com.teamEWSN.gitdeun.common.oauth.dto.provider;

// 제공자마다 반환 형태가 달라서 interface 생성. 제공자별 구현체 필요
public interface OAuth2ResponseDto {

    // 제공자 (Ex. google, github)
    String getProvider();

    // 제공자가 발급해주는 고유 ID
    String getProviderId();

    String getEmail();

    String getName();

    String getProfileImageUrl();

}
