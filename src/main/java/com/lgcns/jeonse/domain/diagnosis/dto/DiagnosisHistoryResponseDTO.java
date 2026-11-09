package com.lgcns.jeonse.domain.diagnosis.dto;

import java.time.LocalDateTime;
import java.util.List;

// docs/03 9번 내 진단 기록. request는 2번 Request Body와 같은 모양
public record DiagnosisHistoryResponseDTO(List<Item> diagnoses) {
    public record Item(Long id, String location, LocalDateTime createdAt, LocalDateTime updatedAt, int score,
                       String grade, String gradeLabel, DiagnosisRequestDTO request) { }
}
