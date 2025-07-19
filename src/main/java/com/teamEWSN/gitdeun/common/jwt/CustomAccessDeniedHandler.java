package com.teamEWSN.gitdeun.common.jwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.teamEWSN.gitdeun.common.exception.ErrorResponse;
import com.teamEWSN.gitdeun.common.exception.ErrorCode;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;


// 권한이 부족한 사용자를 접근 금지 역할(로그인 유무와 상관없이 권한이 없는 경우)
@Slf4j
@RequiredArgsConstructor
@Component
public class CustomAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    @Override
    public void handle(HttpServletRequest request,
        HttpServletResponse response,
        AccessDeniedException accessDeniedException) throws IOException, ServletException {
        log.error("No Authorities", accessDeniedException);
        log.error("Request Uri : {}", request.getRequestURI());

        // ErrorCode 정의
        ErrorCode errorCode = ErrorCode.FORBIDDEN_ACCESS;

        // ErrorResponse 생성
        ErrorResponse errorResponse = ErrorResponse.builder()
            .code(errorCode.getCode())
            .message(errorCode.getMessage())
            .build();

        // HTTP 응답 설정
        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        // 응답 본문에 JSON 데이터 작성
        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }
}
