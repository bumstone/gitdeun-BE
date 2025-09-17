package com.teamEWSN.gitdeun.common.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ErrorCode {
    // 인증 관련
    INVALID_ACCESS_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH-001", "유효하지 않은 액세스 토큰입니다."),
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH-002", "리프레시 토큰이 유효하지 않습니다."),
    TOKEN_NOT_FOUND(HttpStatus.UNAUTHORIZED, "AUTH-003", "요청에 토큰이 존재하지 않습니다."),
    FORBIDDEN_ACCESS(HttpStatus.FORBIDDEN, "AUTH-004", "접근 권한이 없습니다."),
    ACCESS_DENIED(HttpStatus.UNAUTHORIZED, "AUTH-005", "인증되지 않은 유저입니다."),
    INVALID_SECRET_KEY(HttpStatus.UNAUTHORIZED, "AUTH-006", "유효하지 않은 비밀 키입니다."),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "AUTH-007", "유효하지 않은 사용자 정보 또는 비밀번호입니다."),
    ROLE_NOT_FOUND(HttpStatus.FORBIDDEN, "AUTH-008", "권한 정보가 없습니다."),
    NO_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH-009", "토큰이 존재하지 않습니다."),

    // 계정 관련
    USER_NOT_FOUND_BY_ID(HttpStatus.NOT_FOUND, "ACCOUNT-001", "해당 아이디의 회원을 찾을 수 없습니다."),
    USER_NOT_FOUND_BY_EMAIL(HttpStatus.NOT_FOUND, "ACCOUNT-002", "해당 이메일의 회원을 찾을 수 없습니다."),
    ACCOUNT_ALREADY_LINKED(HttpStatus.CONFLICT, "ACCOUNT-003", "이미 다른 사용자와 연동된 소셜 계정입니다."),
    SOCIAL_CONNECTION_NOT_FOUND(HttpStatus.NOT_FOUND, "ACCOUNT-004", "연동된 소셜 계정 정보를 찾을 수 없습니다."),
    USER_SETTING_NOT_FOUND_BY_ID(HttpStatus.NOT_FOUND, "ACCOUNT-005", "해당 아이디의 설정을 찾을 수 없습니다."),

    // 개발 기술 관련
    SKILL_LIMIT_EXCEEDED(HttpStatus.BAD_REQUEST, "SKILL-001", "최대 10개까지만 선택 가능합니다."),
    INVALID_SKILL(HttpStatus.BAD_REQUEST, "SKILL-002", "유효하지 않은 기술입니다."),

    // 소셜 로그인 관련
    OAUTH_PROCESSING_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "OAUTH-001", "소셜 로그인 처리 중 오류가 발생했습니다."),
    UNSUPPORTED_OAUTH_PROVIDER(HttpStatus.BAD_REQUEST, "OAUTH-002", "지원하지 않는 소셜 로그인 제공자입니다."),
    EMAIL_NOT_PROVIDED(HttpStatus.BAD_REQUEST, "OAUTH-003", "소셜 플랫폼에서 이메일 정보를 제공하지 않습니다."),
    OAUTH_COMMUNICATION_FAILED(HttpStatus.BAD_GATEWAY, "OAUTH-004", "소셜 플랫폼과의 통신에 실패했습니다."),
    SOCIAL_TOKEN_REFRESH_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "OAUTH-005", "소셜 플랫폼의 토큰 갱신에 실패했습니다."),
    SOCIAL_ACCOUNT_CONNECT_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "OAUTH-006", "소셜 계정 연동에 실패했습니다."),
    SOCIAL_TOKEN_REFRESH_NOT_SUPPORTED(HttpStatus.BAD_REQUEST, "OAUTH-007", "리프레시 토큰 갱신은 지원하지 않습니다. 재인증이 필요합니다."),

    // 리포지토리 관련
    REPO_NOT_FOUND_BY_ID(HttpStatus.NOT_FOUND, "REPO-001", "해당 ID로 요청한 리포지토리를 찾을 수 없습니다."),
    REPO_NOT_FOUND_BY_URL(HttpStatus.NOT_FOUND, "REPO-002", "해당 URL로 요청한 리포지토리를 찾을 수 없습니다."),

    // 마인드맵 관련
    MINDMAP_NOT_FOUND(HttpStatus.NOT_FOUND, "MINDMAP-001", "요청한 마인드맵을 찾을 수 없습니다."),

    // 프롬프트 히스토리 관련 (신규 추가)
    PROMPT_HISTORY_NOT_FOUND(HttpStatus.NOT_FOUND, "PROMPT-001", "요청한 프롬프트 히스토리를 찾을 수 없습니다."),
    CANNOT_DELETE_APPLIED_PROMPT(HttpStatus.BAD_REQUEST, "PROMPT-002", "적용된 프롬프트 히스토리는 삭제할 수 없습니다."),

    // 멤버 관련
    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "MEMBER-001", "해당 멤버를 찾을 수 없습니다."),
    MEMBER_ALREADY_EXISTS(HttpStatus.CONFLICT, "MEMBER-002", "이미 마인드맵에 등록된 멤버입니다."),
    OWNER_NOT_FOUND(HttpStatus.NOT_FOUND, "MEMBER-003", "해당 소유자를 찾을 수 없습니다."),

    // 초대 관련
    INVITATION_NOT_FOUND(HttpStatus.NOT_FOUND, "INVITE-001", "초대 정보를 찾을 수 없습니다."),
    INVITATION_EXPIRED(HttpStatus.BAD_REQUEST, "INVITE-002", "만료된 초대입니다."),
    INVITATION_ALREADY_EXISTS(HttpStatus.CONFLICT, "INVITE-003", "이미 초대 대기 중인 사용자입니다."),
    CANNOT_INVITE_SELF(HttpStatus.BAD_REQUEST, "INVITE-004", "자기 자신을 초대할 수 없습니다."),
    INVITATION_REJECTED_USER(HttpStatus.FORBIDDEN, "INVITE-005", "초대를 거절한 사용자이므로 초대할 수 없습니다."),

    // 알림 관련
    NOTIFICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "NOTIFICATION-001", "알림을 찾을 수 없습니다."),
    CANNOT_ACCESS_NOTIFICATION(HttpStatus.FORBIDDEN, "NOTIFICATION-002", "해당 알림에 접근할 권한이 없습니다."),


    // 방문기록 관련
    HISTORY_NOT_FOUND(HttpStatus.NOT_FOUND, "VISITHISTORY-001", "방문 기록을 찾을 수 없습니다."),

    // 방문 기록 핀 고정 관련
    USER_NOT_FOUND_FIX_PIN(HttpStatus.NOT_FOUND, "PINNEDHISTORY-001", "핀 고정한 유저를 찾을 수 없습니다."),
    PINNEDHISTORY_ALREADY_EXISTS(HttpStatus.CONFLICT, "PINNEDHISTORY-002", "이미 핀 고정한 기록입니다."),
    PINNEDHISTORY_NOT_FOUND(HttpStatus.NOT_FOUND, "PINNEDHISTORY-003", "핀 고정 기록을 찾을 수 없습니다."),
    PINNED_HISTORY_LIMIT_EXCEEDED(HttpStatus.BAD_REQUEST, "PINNEDHISTORY-004", "최대 8개까지만 핀으로 고정할 수 있습니다."),

    // 모집 공고 관련
    RECRUITMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "RECRUIT-001", "모집 공고를 찾을 수 없습니다."),
    RECRUITMENT_NOT_RECRUITING(HttpStatus.BAD_REQUEST, "RECRUIT-002", "모집 중인 공고가 아닙니다."),
    RECRUITMENT_EXPIRED(HttpStatus.BAD_REQUEST, "RECRUIT-003", "모집 기간이 마감된 공고입니다."),
    RECRUITMENT_FULL(HttpStatus.BAD_REQUEST, "RECRUIT-004", "모집 인원이 마감되었습니다."),
    INVALID_DATE_RANGE(HttpStatus.BAD_REQUEST, "RECRUIT-005", "시작일은 종료일보다 이전이어야 합니다."),
    END_DATE_IN_PAST(HttpStatus.BAD_REQUEST, "RECRUIT-006", "종료일은 현재보다 이후여야 합니다."),

    // 지원 관련
    APPLICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "APPLICATION-001", "요청한 지원 정보를 찾을 수 없습니다."),
    ALREADY_APPLIED(HttpStatus.CONFLICT, "APPLICATION-002", "이미 지원한 공고입니다."),
    CANNOT_APPLY_OWN_RECRUITMENT(HttpStatus.BAD_REQUEST, "APPLICATION-003", "본인의 공고에는 지원할 수 없습니다."),
    INVALID_APPLICATION_FIELD(HttpStatus.BAD_REQUEST, "APPLICATION-004", "해당 공고에서 모집하지 않는 분야입니다."),
    APPLICATION_ALREADY_WITHDRAWN(HttpStatus.BAD_REQUEST, "APPLICATION-005", "이미 철회된 지원입니다."),
    CANNOT_WITHDRAW_ACCEPTED(HttpStatus.BAD_REQUEST, "APPLICATION-006", "수락된 지원은 철회할 수 없습니다."),
    APPLICATION_NOT_ACTIVE(HttpStatus.BAD_REQUEST, "APPLICATION-007", "활성 상태가 아닌 지원입니다."),
    APPLICATION_ALREADY_PROCESSED(HttpStatus.BAD_REQUEST, "APPLICATION-008", "이미 처리된 지원입니다."),


    // S3 파일 관련
    // Client Errors (4xx)
    FILE_NOT_FOUND(HttpStatus.NOT_FOUND, "FILE-001", "요청한 파일을 찾을 수 없습니다."),
    INVALID_FILE_LIST(HttpStatus.BAD_REQUEST, "FILE-002", "파일 목록이 비어있거나 유효하지 않습니다."),
    INVALID_FILE_PATH(HttpStatus.BAD_REQUEST, "FILE-003", "파일 경로나 이름이 유효하지 않습니다."),
    INVALID_FILE_TYPE(HttpStatus.BAD_REQUEST, "FILE-004", "지원하지 않는 파일 형식입니다."),
    FILE_COUNT_EXCEEDED(HttpStatus.BAD_REQUEST, "FILE-005", "업로드 가능한 파일 개수를 초과했습니다."),
    FILE_SIZE_EXCEEDED(HttpStatus.BAD_REQUEST, "FILE-006", "파일 크기가 허용된 용량을 초과했습니다."),
    INVALID_S3_URL(HttpStatus.BAD_REQUEST, "FILE-007", "S3 URL 형식이 올바르지 않습니다."),

    // Server Errors (5xx)
    FILE_UPLOAD_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "FILE-501", "파일 업로드 중 서버 오류가 발생했습니다."),
    FILE_DOWNLOAD_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "FILE-502", "파일 다운로드 중 서버 오류가 발생했습니다."),
    FILE_DELETE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "FILE-503", "파일 삭제 중 서버 오류가 발생했습니다."),
    FILE_MOVE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "FILE-504", "파일 이동 중 서버 오류가 발생했습니다.");

    

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

}