package com.teamEWSN.gitdeun.user.service;

import com.teamEWSN.gitdeun.common.exception.ErrorCode;
import com.teamEWSN.gitdeun.common.exception.GlobalException;
import com.teamEWSN.gitdeun.user.dto.UserSettingResponseDto;
import com.teamEWSN.gitdeun.user.dto.UserSettingUpdateRequestDto;
import com.teamEWSN.gitdeun.user.entity.User;
import com.teamEWSN.gitdeun.user.entity.UserSetting;
import com.teamEWSN.gitdeun.user.repository.UserRepository;
import com.teamEWSN.gitdeun.user.repository.UserSettingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true) // 기본적으로 읽기 전용 트랜잭션 사용
public class UserSettingService {

    private final UserSettingRepository userSettingRepository;
    private final UserRepository userRepository;

    /**
     * 사용자 ID로 설정 정보를 조회합니다.
     * 설정이 없는 경우 기본값을 생성하고 저장한 뒤 반환합니다.
     * @param userId 조회할 사용자의 ID
     * @return 사용자의 설정 정보 DTO
     */
    @Transactional // 이 메서드는 쓰기 작업이 발생할 수 있으므로 @Transactional을 명시
    public UserSettingResponseDto getSettings(Long userId) {
        UserSetting userSetting = userSettingRepository.findByUserId(userId)
            .orElseGet(() -> {
                // 설정이 없는 경우, 기본 설정을 생성
                User user = userRepository.findById(userId)
                    .orElseThrow(() -> new GlobalException(ErrorCode.USER_NOT_FOUND_BY_ID));
                UserSetting defaultSetting = UserSetting.createDefault(user);
                return userSettingRepository.save(defaultSetting);
            });

        return new UserSettingResponseDto(
            userSetting.getTheme(),
            userSetting.getMode(),
            userSetting.isEmailNotification()
        );
    }

    /**
     * 사용자 설정을 업데이트합니다.
     * @param userId 업데이트할 사용자의 ID
     * @param requestDto 업데이트할 설정 내용
     * @return 업데이트된 사용자의 설정 정보 DTO
     */
    @Transactional // 쓰기 작업을 위한 @Transactional
    public UserSettingResponseDto updateSettings(Long userId, UserSettingUpdateRequestDto requestDto) {
        // findByUserId를 사용하여 UserSetting을 직접 찾습니다.
        UserSetting userSetting = userSettingRepository.findByUserId(userId)
            .orElseThrow(() -> new GlobalException(ErrorCode.USER_SETTING_NOT_FOUND_BY_ID));

        // 엔티티의 update 메서드를 사용하여 상태 변경
        userSetting.update(requestDto);

        // 변경된 내용을 DTO로 변환하여 반환 (userSettingRepository.save()는 필요 없음)
        // 트랜잭션이 커밋될 때 변경 감지(Dirty Checking)에 의해 자동으로 DB에 반영됩니다.
        return new UserSettingResponseDto(
            userSetting.getTheme(),
            userSetting.getMode(),
            userSetting.isEmailNotification()
        );
    }
}

