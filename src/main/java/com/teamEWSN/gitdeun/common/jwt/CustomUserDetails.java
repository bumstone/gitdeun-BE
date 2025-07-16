package com.teamEWSN.gitdeun.common.jwt;

import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import com.teamEWSN.gitdeun.user.entity.Role;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

/**
 * 인증된 사용자의 신분증(사용자의 정보)
 * JwtAuthenticationFilter 토큰 정보를 바탕으로 객체 생성
 */
@Getter
public class CustomUserDetails implements UserDetails, CustomUserPrincipal {
    private final Long id;
    private final String email;
    private final String nickname;
    private final String profileImage;
    private final Role role;
    private final String name;

    public CustomUserDetails(Long id, String email, String nickname, String profileImage, Role role, String name) {
        this.id = id;
        this.email = email;
        this.nickname = nickname;
        this.profileImage = profileImage;
        this.role = role;
        this.name = name;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.singletonList(new SimpleGrantedAuthority(role.name())); // 문자열 기반 권한
    }

    @Override
    public String getRole() {
        return role.name();
    }

    @Override
    public Map<String, Object> getAttributes() {
        return Collections.emptyMap(); // OAuth2가 아니므로 빈 맵 반환
    }

    @Override
    public String getPassword() {
        return null; // User 엔티티에 비밀번호가 없으므로 null 반환
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}