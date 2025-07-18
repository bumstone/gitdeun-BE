package com.teamEWSN.gitdeun.user.service;

import com.teamEWSN.gitdeun.common.exception.GlobalException;
import com.teamEWSN.gitdeun.common.exception.ErrorCode;
import com.teamEWSN.gitdeun.common.jwt.BlacklistService;
import com.teamEWSN.gitdeun.common.jwt.RefreshTokenService;
import com.teamEWSN.gitdeun.common.oauth.service.GitHubApiHelper;
import com.teamEWSN.gitdeun.common.oauth.service.GoogleApiHelper;
import com.teamEWSN.gitdeun.common.oauth.entity.SocialConnection;
import com.teamEWSN.gitdeun.user.dto.UserResponseDto;
import com.teamEWSN.gitdeun.user.entity.User;
import com.teamEWSN.gitdeun.user.mapper.UserMapper;
import com.teamEWSN.gitdeun.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {
    private final UserMapper userMapper;
    private final RefreshTokenService refreshTokenService;
    private final BlacklistService blacklistService;
    private final UserRepository userRepository;
    private final GoogleApiHelper googleApiHelper;
    private final GitHubApiHelper gitHubApiHelper;

    // 회원 정보 조회
    @Transactional(readOnly = true)
    public UserResponseDto getMyInfo(Long userId) {
        User user = userRepository.findByIdAndDeletedAtIsNull(userId)
            .orElseThrow(() -> new GlobalException(ErrorCode.USER_NOT_FOUND_BY_ID));

        return userMapper.toResponseDto(user);
    }


    // 로그인된 회원 탈퇴 처리
    @Transactional
    public void deleteUser(Long userId, String accessToken, String refreshToken) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new GlobalException(ErrorCode.USER_NOT_FOUND_BY_ID));

        List<SocialConnection> connections = user.getSocialConnections();

        // 모든 소셜 연동 해제 시도
        for (SocialConnection connection : connections) {
            try {
                switch (connection.getProvider()) {
                    case GOOGLE:
                        googleApiHelper.revokeToken(connection.getAccessToken()).block();
                        log.info("Google token for user {} has been revoked.", userId);
                        break;
                    case GITHUB:
                        gitHubApiHelper.revokeToken(connection.getAccessToken()).block();
                        log.info("GitHub token for user {} has been revoked.", userId);
                        break;
                    default:
                        log.warn("Unsupported provider for token revocation: {}", connection.getProvider());
                }
            } catch (Exception e) {
                // 특정 플랫폼 연동 해제에 실패하더라도, 다른 플랫폼 및 DB 처리는 계속 진행하기 위해 로그만 남김
                log.error("Failed to revoke token for provider {} and user {}: {}",
                    connection.getProvider(), userId, e.getMessage());
            }
        }

        // redis에서 리프레시 토큰 삭제
        refreshTokenService.deleteRefreshToken(refreshToken);
        // access token 블랙리스트에 등록
        blacklistService.addToBlacklist(accessToken);

        // 깃든 서비스 DB에서 soft-delete 처리
        user.markAsDeleted();
        userRepository.save(user);
        log.info("User {} has been marked as deleted.", userId);
    }

    // 이메일로 회원 검색
    @Transactional(readOnly = true)
    public User findUserByEmail(String email) {
        return userRepository.findByEmailAndDeletedAtIsNull(email)
            .orElseThrow(() -> new GlobalException(ErrorCode.USER_NOT_FOUND_BY_EMAIL));

    }
}