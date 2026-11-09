package com.lgcns.jeonse.domain.diagnosis.dto;

import com.lgcns.jeonse.domain.diagnosis.entity.RiskGrade;
import com.lgcns.jeonse.domain.question.entity.RegistryQuestion;
import java.math.BigDecimal;
import java.util.List;

// 점수 계산 결과를 결과 문구(advice)에 넘기는 내부 형식 (docs/be1-be2-diagnosis-handoff.md)
public record ScoreResult(int score, RiskGrade grade, BigDecimal debtRatio, int debtRatioScore,
                          List<ScoredAnswer> scoredAnswers, int mortgageMax, boolean seniorDepositMissing) {
    // scoredAnswers는 질문 순서(order)대로
    public record ScoredAnswer(RegistryQuestion question, String answer, int score) {
        public String code() { return question.getCode(); }
    }
}
