package com.teamEWSN.gitdeun.common.jwt;

import java.util.Map;

public interface CustomUserPrincipal {
    Long getId();
    String getEmail();
    String getNickname();
    String getRole();
    String getName();
    String getProfileImage();
    Map<String, Object> getAttributes();

}
