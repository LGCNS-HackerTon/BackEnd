package com.lgcns.jeonse.global.exception;

public class OcrUnreadableException extends RuntimeException {
    public OcrUnreadableException() {
        super("서류를 읽지 못했어요. 더 선명한 파일을 다시 올리거나 질문에 직접 답해 주세요.");
    }
}
