package com.teamEWSN.gitdeun.common.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ErrorCode {
    // 인증 관련
    INVALID_PASSWORD(HttpStatus.UNAUTHORIZED, "AUTH-001", "비밀번호가 일치하지 않습니다."),
    REFRESH_TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "AUTH-002", "리프레시 토큰이 만료되었습니다."),
    NO_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH-003", "토큰이 존재하지 않습니다."),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH-004", "유효하지 않은 토큰입니다."),
    ACCESS_DENIED(HttpStatus.UNAUTHORIZED, "AUTH-005", "인증되지 않은 유저입니다."),
    DELETE_USER_DENIED(HttpStatus.FORBIDDEN, "AUTH-006", "회원 탈퇴가 거부되었습니다."),
    ROLE_NOT_FOUND(HttpStatus.FORBIDDEN, "AUTH-007", "권한 정보가 없습니다."),


    // 계정 관련
    DUPLICATED_REAL_ID(HttpStatus.CONFLICT, "ACCOUNT-001", "이미 존재하는 아이디입니다."),
    USER_NOT_FOUND_BY_REAL_ID(HttpStatus.NOT_FOUND, "ACCOUNT-002", "해당 아이디의 회원을 찾을 수 없습니다."),
    FORBIDDEN_ACCESS(HttpStatus.FORBIDDEN, "AUTH-011", "잘못된 접근입니다."),


    // S3 파일 관련
    FILE_COUNT_EXCEEDED(HttpStatus.BAD_REQUEST, "FILE-001", "업로드 가능한 파일 개수를 초과했습니다."),
    FILE_SIZE_EXCEEDED(HttpStatus.BAD_REQUEST, "FILE-002", "파일 크기가 허용된 용량을 초과했습니다."),
    INVALID_FILE_TYPE(HttpStatus.BAD_REQUEST, "FILE-003", "지원하지 않는 파일 형식입니다."),
    FILE_NOT_FOUND(HttpStatus.NOT_FOUND, "FILE-004", "요청한 파일을 찾을 수 없습니다."),
    FILE_UPLOAD_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "FILE-006", "파일 업로드 중 오류가 발생했습니다."),
    INVALID_FILE_LIST(HttpStatus.BAD_REQUEST, "FILE-006", "파일 목록이 비어있거나 유효하지 않습니다."),
    INVALID_FILE_PATH(HttpStatus.BAD_REQUEST, "FILE-007", "파일 경로나 이름이 유효하지 않습니다."),
    FILE_MOVE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "FILE-008", "파일 이동 중 오류가 발생했습니다.");

    

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

}