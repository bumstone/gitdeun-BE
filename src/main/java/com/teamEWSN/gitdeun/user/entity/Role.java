package com.teamEWSN.gitdeun.user.entity;

import org.springframework.security.core.GrantedAuthority;

public enum Role implements GrantedAuthority {
    ROLE_USER,
    ROLE_ADMIN, USER;

    @Override
    public String getAuthority() {
        return name();
    }
}