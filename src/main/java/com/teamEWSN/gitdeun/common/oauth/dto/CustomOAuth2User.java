package com.teamEWSN.gitdeun.common.oauth.dto;

import com.teamEWSN.gitdeun.user.entity.Role;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

@Getter
public class CustomOAuth2User implements OAuth2User {

    private final Long userId;
    private final Role role;

    public CustomOAuth2User(Long userId, Role role) {
        this.userId = userId;
        this.role = role;
    }

    @Override
    public Map<String, Object> getAttributes() {
        // 인증 성공 후 시스템 내부에서만 사용, 소셜 플랫폼의 attributes 필요 x
        return Collections.emptyMap();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.singletonList(new SimpleGrantedAuthority(this.role.name()));
    }

    @Override
    public String getName() {
        // Spring Security에서 Principal의 이름을 식별하기 위해 우리 서비스의 고유 ID로 사용
        return String.valueOf(this.userId);
    }

}