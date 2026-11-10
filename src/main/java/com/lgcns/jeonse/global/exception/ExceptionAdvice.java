package com.lgcns.jeonse.global.exception;

import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import com.lgcns.jeonse.global.apiPayload.ApiError;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import org.apache.tomcat.util.http.fileupload.impl.FileSizeLimitExceededException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class ExceptionAdvice {
    private static final Logger log = LoggerFactory.getLogger(ExceptionAdvice.class);

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
                "INVALID_INPUT", exception.getRequiredType() == LocalDate.class
                        ? "날짜는 YYYY-MM-DD 형식으로 입력해 주세요." : "숫자로 입력해 주세요.", exception.getName())));
    }

    // JSON 형식 오류는 field null, 타입이 틀린 값(소수 금액 등)은 그 최상위 필드 이름 (docs/03 2절)
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> unreadableBody(HttpMessageNotReadableException exception) {
        String field = exception.getCause() instanceof MismatchedInputException mismatch && !mismatch.getPath().isEmpty()
                ? mismatch.getPath().get(0).getFieldName() : null;
        return ResponseEntity.badRequest().body(new ApiError(new ApiError.ErrorDetail(
                "INVALID_INPUT", "입력 형식이 올바르지 않아요. 다시 확인해 주세요.", field)));
    }

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiError> apiException(ApiException exception) {
        return ResponseEntity.status(exception.getStatus()).body(new ApiError(
                new ApiError.ErrorDetail(exception.getCode(), exception.getMessage(), exception.getField())));
    }

    @ExceptionHandler(OcrUnreadableException.class)
    public ResponseEntity<ApiError> ocrUnreadable(OcrUnreadableException exception) {
        return ResponseEntity.unprocessableEntity().body(new ApiError(
                new ApiError.ErrorDetail("OCR_UNREADABLE", exception.getMessage(), null)));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiError> fileTooLarge(MaxUploadSizeExceededException exception) {
        String field = null; // 파일 하나가 넘쳤을 때만 Tomcat이 필드 이름을 알려 준다
        for (Throwable t = exception; t != null; t = t.getCause()) {
            if (t instanceof FileSizeLimitExceededException e) field = e.getFieldName();
        }
        return ResponseEntity.badRequest().body(new ApiError(
                new ApiError.ErrorDetail("INVALID_INPUT", "서류는 파일당 10MB 이하로 올려 주세요.", field)));
    }

    // 없는 경로 (docs/03 2절 NOT_FOUND). Boot 3.2부터 없는 경로는 NoResourceFoundException으로 온다
    @ExceptionHandler({NoResourceFoundException.class, NoHandlerFoundException.class})
    public ResponseEntity<ApiError> notFound(Exception exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiError(new ApiError.ErrorDetail(
                "NOT_FOUND", "요청한 주소를 찾을 수 없어요. 주소를 다시 확인해 주세요.", null)));
    }

    /**
     * 위에서 처리하지 못한 예외는 500 {@code INTERNAL_ERROR}로 응답한다 (docs/03 2장).
     * 응답에는 내부 메시지를 넣지 않고, 원인을 찾을 수 있게 요청(메서드·경로)과 예외(stack trace 포함)를 ERROR 로그로 남긴다.
     * 쿼리 문자열과 요청 본문은 개인정보가 들어갈 수 있어 로그에 남기지 않는다.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> internalError(Exception exception, HttpServletRequest request) {
        log.error("처리하지 못한 서버 오류: {} {}", request.getMethod(), request.getRequestURI(), exception);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ApiError(
                new ApiError.ErrorDetail("INTERNAL_ERROR", "서버 오류가 발생했어요. 잠시 후 다시 시도해 주세요.", null)));
    }
}
