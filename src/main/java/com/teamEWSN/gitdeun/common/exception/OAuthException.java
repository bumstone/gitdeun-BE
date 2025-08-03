package com.teamEWSN.gitdeun.common.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class OAuthException extends RuntimeException {
    ErrorCode errorCode;
}
