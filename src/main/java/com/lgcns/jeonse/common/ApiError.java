package com.lgcns.jeonse.common;

public record ApiError(ErrorDetail error) {
    public record ErrorDetail(String code, String message, String field) { }
}
