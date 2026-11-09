package com.lgcns.jeonse.global.exception;

import org.springframework.http.HttpStatus;

/** docs/03 공통 에러 중 400 INVALID_INPUT 외의 코드(401·404·409 등)를 돌려줄 때 쓴다. */
public class ApiException extends RuntimeException {
    private final HttpStatus status;
    private final String code;
    private final String field;

    public ApiException(HttpStatus status, String code, String message, String field) {
        super(message);
        this.status = status;
        this.code = code;
        this.field = field;
    }

    public static ApiException unauthorized() {
        return new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "로그인이 필요해요. 다시 로그인해 주세요.", null);
    }

    public HttpStatus getStatus() { return status; }
    public String getCode() { return code; }
    public String getField() { return field; }
}
