package com.lgcns.jeonse.domain.diagnosis.advice;

import com.lgcns.jeonse.domain.diagnosis.dto.DiagnosisResponseDTO;
import java.util.List;

public record AdviceResult(String gradeLabel, String gradeMessage, List<DiagnosisResponseDTO.Factor> factors,
                           DiagnosisResponseDTO.CommonAdvice commonAdvice, String aiExplanation, boolean aiUsed,
                           List<String> warnings, String disclaimer) { }
