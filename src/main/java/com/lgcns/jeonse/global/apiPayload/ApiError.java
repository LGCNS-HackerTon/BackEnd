package com.lgcns.jeonse.global.apiPayload;

public record ApiError(ErrorDetail error) {
    public record ErrorDetail(String code, String message, String field) { }
}
