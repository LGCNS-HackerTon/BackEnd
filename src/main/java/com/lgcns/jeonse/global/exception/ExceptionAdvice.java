package com.lgcns.jeonse.global.exception;

import com.lgcns.jeonse.global.apiPayload.ApiError;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class ExceptionAdvice {
    @ExceptionHandler(InvalidInputException.class)
    public ResponseEntity<ApiError> invalidInput(InvalidInputException exception) {
        return ResponseEntity.badRequest().body(new ApiError(
                new ApiError.ErrorDetail("INVALID_INPUT", exception.getMessage(), exception.getField())));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiError> missingParameter(MissingServletRequestParameterException exception) {
        return ResponseEntity.badRequest().body(new ApiError(new ApiError.ErrorDetail(
                "INVALID_INPUT", exception.getParameterName() + "을(를) 입력해 주세요.", exception.getParameterName())));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> invalidParameter(MethodArgumentTypeMismatchException exception) {
        return ResponseEntity.badRequest().body(new ApiError(new ApiError.ErrorDetail(
                "INVALID_INPUT", "날짜는 YYYY-MM-DD 형식으로 입력해 주세요.", exception.getName())));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> internalError(Exception exception) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ApiError(
                new ApiError.ErrorDetail("INTERNAL_ERROR", "서버 오류가 발생했어요. 잠시 후 다시 시도해 주세요.", null)));
    }
}
