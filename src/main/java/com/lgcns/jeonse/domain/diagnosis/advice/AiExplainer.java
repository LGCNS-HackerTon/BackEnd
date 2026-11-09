package com.lgcns.jeonse.domain.diagnosis.advice;

import com.lgcns.jeonse.domain.diagnosis.dto.DiagnosisResponseDTO;
import com.lgcns.jeonse.domain.diagnosis.dto.ScoreResult;
import java.util.List;
import java.util.Optional;

// 템플릿 설명을 쉬운 말로 다듬는다 (docs/08 AI-01). 실패·미설정이면 empty → 템플릿 그대로 (AI-02)
public interface AiExplainer {
    Optional<String> polish(String template, ScoreResult result, List<DiagnosisResponseDTO.Factor> factors);
}
