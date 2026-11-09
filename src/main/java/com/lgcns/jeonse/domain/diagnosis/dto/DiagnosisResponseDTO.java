package com.lgcns.jeonse.domain.diagnosis.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record DiagnosisResponseDTO(Long id, int score, String grade, String gradeLabel, String gradeMessage,
                                   BigDecimal debtRatio, List<Factor> factors, CommonAdvice commonAdvice,
                                   String aiExplanation, boolean aiUsed, List<String> warnings, String disclaimer,
                                   LocalDateTime createdAt) {
    public record Factor(String code, int score, String title, String explanation, String askAgent,
                         String specialClause) { }

    public record CommonAdvice(String askAgent, String specialClause) { }
}
