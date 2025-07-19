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

    // 소셜 로그인 관련
    OAUTH_PROCESSING_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "OAUTH-001", "소셜 로그인 처리 중 오류가 발생했습니다."),
    UNSUPPORTED_OAUTH_PROVIDER(HttpStatus.BAD_REQUEST, "OAUTH-002", "지원하지 않는 소셜 로그인 제공자입니다."),
    EMAIL_NOT_PROVIDED(HttpStatus.BAD_REQUEST, "OAUTH-003", "소셜 플랫폼에서 이메일 정보를 제공하지 않습니다."),
    OAUTH_COMMUNICATION_FAILED(HttpStatus.BAD_GATEWAY, "OAUTH-004", "소셜 플랫폼과의 통신에 실패했습니다."),
    SOCIAL_TOKEN_REFRESH_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "OAUTH-005", "소셜 플랫폼의 토큰 갱신에 실패했습니다."),
    SOCIAL_ACCOUNT_CONNECT_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "OAUTH-006", "소셜 계정 연동에 실패했습니다."),
    GITHUB_TOKEN_REFRESH_NOT_SUPPORTED(HttpStatus.BAD_REQUEST, "OAUTH-007", "GitHub 토큰 갱신은 지원하지 않습니다. 재인증이 필요합니다."),

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