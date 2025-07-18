package com.teamEWSN.gitdeun.user.dto;

import com.teamEWSN.gitdeun.user.entity.Role;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Builder
public class UserResponseDto {
    private Long id;          // 사용자 ID
    private String name;        // 사용자 이름
    private String email;     // 이메일
    private String nickname;  // 닉네임
    private String profileImage;    // image url
    private Role role;      // 권한 (USER/ADMIN 등)


}