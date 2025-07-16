package com.teamEWSN.gitdeun.common.util;

import com.teamEWSN.gitdeun.common.jwt.CustomUserDetails;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;


/**
 *  현재 인증된 사용자의 정보를 가져옴
 *  컨트롤러가 아닌 다른 서비스 계층 등에서 사용자 ID가 필요할 때 편리하게 사용
 */
@Component
public class AuthenticateUser {
  public Long authenticateUserId() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

    if (authentication == null || !authentication.isAuthenticated() || authentication.getPrincipal().equals("anonymousUser")) {
      return 0L; // 인증되지 않은 사용자일 경우 0 반환
    }

    // 인증된 사용자의 경우 userId 반환
    CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
    return userDetails.getId();
  }
}
