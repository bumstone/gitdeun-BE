package com.teamEWSN.gitdeun.user.service;

import com.teamEWSN.gitdeun.common.exception.ErrorCode;
import com.teamEWSN.gitdeun.common.exception.GlobalException;
import com.teamEWSN.gitdeun.user.dto.UserSettingResponseDto;
import com.teamEWSN.gitdeun.user.dto.UserSettingUpdateRequestDto;
import com.teamEWSN.gitdeun.user.entity.User;
import com.teamEWSN.gitdeun.user.entity.UserSetting;
import com.teamEWSN.gitdeun.user.mapper.UserSettingMapper;
import com.teamEWSN.gitdeun.user.repository.UserRepository;
import com.teamEWSN.gitdeun.user.repository.UserSettingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserSettingService {

    private final UserSettingRepository userSettingRepository;
    private final UserRepository userRepository;
    private final UserSettingMapper userSettingMapper;

    /**
     * 사용자 ID로 설정 정보 조회
     * 설정이 없는 경우 기본값을 생성하고 저장한 뒤 반환합니다.
     * @param userId 조회할 사용자의 ID
     * @return 사용자의 설정 정보 DTO
     */
    @Transactional
    public UserSettingResponseDto getSettings(Long userId) {
        UserSetting userSetting = userSettingRepository.findByUserId(userId)
            .orElseGet(() -> {
                // 설정이 없는 경우, 기본 설정을 생성
                User user = userRepository.findById(userId)
                    .orElseThrow(() -> new GlobalException(ErrorCode.USER_NOT_FOUND_BY_ID));
                UserSetting defaultSetting = UserSetting.createDefault(user);
                return userSettingRepository.save(defaultSetting);
            });

        return userSettingMapper.toResponseDto(userSetting);
    }

    /**
     * 사용자 설정 업데이트
     * @param userId 업데이트할 사용자의 ID
     * @param requestDto 업데이트할 설정 내용
     * @return 업데이트된 사용자의 설정 정보 DTO
     */
    @Transactional
    public UserSettingResponseDto updateSettings(Long userId, UserSettingUpdateRequestDto requestDto) {
        UserSetting userSetting = userSettingRepository.findByUserId(userId)
            .orElseThrow(() -> new GlobalException(ErrorCode.USER_SETTING_NOT_FOUND_BY_ID));

        // 엔티티의 update 메서드를 사용하여 상태 변경
        userSetting.update(requestDto);

        return userSettingMapper.toResponseDto(userSetting);
    }
}

