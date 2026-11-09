package com.lgcns.jeonse.domain.diagnosis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lgcns.jeonse.domain.diagnosis.advice.AdviceResult;
import com.lgcns.jeonse.domain.diagnosis.advice.AdviceService;
import com.lgcns.jeonse.domain.diagnosis.advice.ClaudeAiExplainer;
import com.lgcns.jeonse.domain.diagnosis.dto.DiagnosisRequestDTO;
import com.lgcns.jeonse.domain.diagnosis.dto.DiagnosisResponseDTO;
import com.lgcns.jeonse.domain.diagnosis.dto.DiagnosisResponseDTO.Factor;
import com.lgcns.jeonse.domain.diagnosis.entity.RiskGrade;
import com.lgcns.jeonse.domain.diagnosis.service.DiagnosisService;
import com.lgcns.jeonse.global.exception.InvalidInputException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "anthropic.api-key=")
class DiagnosisServiceTest {
    @Autowired DiagnosisService service;

    // 모든 질문에 안전한 답
    static Map<String, String> safeAnswers() {
        Map<String, String> answers = new LinkedHashMap<>();
        answers.put("T1_ADDRESS", "YES");
        answers.put("T2_USAGE", "YES");
        answers.put("A1_OWNER", "YES");
        answers.put("A2_RECENT_OWNER", "NO");
        answers.put("A3_TRUST", "NO");
        answers.put("A4_SEIZURE", "NO");
        answers.put("B1_MORTGAGE", "NO");
        answers.put("B2_LEASE_REG", "NO");
        answers.put("B3_JEONSE_RIGHT", "NO");
        answers.put("R1_VIOLATION", "NO");
        return answers;
    }

    static DiagnosisRequestDTO request(Integer deposit, Integer salePrice, Integer mortgageMax, Integer seniorDeposit,
                                       Map<String, String> answers) {
        List<DiagnosisRequestDTO.Answer> list = new ArrayList<>();
        answers.forEach((code, answer) -> list.add(new DiagnosisRequestDTO.Answer(code, answer)));
        return new DiagnosisRequestDTO(deposit, salePrice, mortgageMax, seniorDeposit, list);
    }

    static DiagnosisRequestDTO caseB(Integer seniorDeposit) {
        Map<String, String> answers = safeAnswers();
        answers.put("T2_USAGE", "NO");
        answers.put("A2_RECENT_OWNER", "YES");
        answers.put("B1_MORTGAGE", "YES");
        return request(24000, 28000, 3600, seniorDeposit, answers);
    }

    @Test
    void caseA_cleanApartment_is0Safe() {
        DiagnosisResponseDTO result = service.diagnose(request(20000, 30000, 0, 0, safeAnswers()));
        assertThat(result.score()).isZero();
        assertThat(result.grade()).isEqualTo("SAFE");
        assertThat(result.debtRatio()).isEqualByComparingTo("66.7");
        assertThat(result.factors()).isEmpty();
        assertThat(result.warnings()).isEmpty();
        assertThat(result.id()).isNull();
        assertThat(result.aiUsed()).isFalse();
        assertThat(result.disclaimer()).isEqualTo("참고용이며 법적 판단이 아닙니다. 계약 전 공인중개사·전문가에게 확인하세요.");
    }

    @Test
    void caseB_neighborhoodVilla_is70Danger() {
        DiagnosisResponseDTO result = service.diagnose(caseB(0));
        assertThat(result.score()).isEqualTo(70);
        assertThat(result.grade()).isEqualTo("DANGER");
        assertThat(result.gradeLabel()).isEqualTo("위험");
        assertThat(result.debtRatio()).isEqualByComparingTo("98.6");
        assertThat(result.factors()).extracting(Factor::code)
                .containsExactly("DEBT_RATIO", "T2_USAGE", "A2_RECENT_OWNER", "B1_MORTGAGE");
        assertThat(result.factors()).extracting(Factor::score).containsExactly(40, 20, 10, 0);
        assertThat(result.factors().get(0).title()).isEqualTo("부채비율 98.6%");
        assertThat(result.factors().get(3).title()).isEqualTo("근저당권 있음 (채권최고액 3,600만 원)");
        assertThat(result.factors().get(3).specialClause()).contains("근저당권을 말소");
        assertThat(result.warnings()).isEmpty();
        assertThat(result.commonAdvice().askAgent()).isEqualTo("국세·지방세 체납은 없으신가요?");
    }

    @Test
    void caseB_withoutSeniorDeposit_sameScoreWithWarning() {
        DiagnosisResponseDTO result = service.diagnose(caseB(null));
        assertThat(result.score()).isEqualTo(70);
        assertThat(result.debtRatio()).isEqualByComparingTo("98.6");
        assertThat(result.warnings())
                .containsExactly("선순위 보증금을 모르면 0으로 계산했어요. 다가구주택이라면 중개사에게 꼭 물어보세요.");
    }

    @Test
    void caseC_trustOfficetel_is40Caution() {
        Map<String, String> answers = safeAnswers();
        answers.put("A3_TRUST", "YES");
        DiagnosisResponseDTO result = service.diagnose(request(15000, 20000, null, 0, answers));
        assertThat(result.score()).isEqualTo(40);
        assertThat(result.grade()).isEqualTo("CAUTION");
        assertThat(result.debtRatio()).isEqualByComparingTo("75.0");
        assertThat(result.factors()).extracting(Factor::code).containsExactly("A3_TRUST", "DEBT_RATIO");
        assertThat(result.factors().get(1).title()).isEqualTo("부채비율 75%");
    }

    @Test
    void gradeBoundaries() {
        assertThat(RiskGrade.of(0)).isEqualTo(RiskGrade.SAFE);
        assertThat(RiskGrade.of(29)).isEqualTo(RiskGrade.SAFE);
        assertThat(RiskGrade.of(30)).isEqualTo(RiskGrade.CAUTION);
        assertThat(RiskGrade.of(59)).isEqualTo(RiskGrade.CAUTION);
        assertThat(RiskGrade.of(60)).isEqualTo(RiskGrade.DANGER);
    }

    @Test
    void singleSixtyPointItem_isDanger_and30IsCaution() {
        Map<String, String> answers = safeAnswers();
        answers.put("B2_LEASE_REG", "YES");
        assertThat(service.diagnose(request(10000, 30000, 0, 0, answers)).grade()).isEqualTo("DANGER");

        answers = safeAnswers();
        answers.put("A1_OWNER", "NO");
        DiagnosisResponseDTO caution = service.diagnose(request(10000, 30000, 0, 0, answers));
        assertThat(caution.score()).isEqualTo(30);
        assertThat(caution.grade()).isEqualTo("CAUTION");

        answers.put("A2_RECENT_OWNER", "YES");
        answers.put("R1_VIOLATION", "YES"); // 30 + 10 + 20 = 60
        assertThat(service.diagnose(request(10000, 30000, 0, 0, answers)).grade()).isEqualTo("DANGER");
    }

    @Test
    void unknown_isScoredAsRisky_withAgentNote() {
        Map<String, String> answers = safeAnswers();
        answers.put("T1_ADDRESS", "UNKNOWN");
        answers.put("A2_RECENT_OWNER", "UNKNOWN");
        DiagnosisResponseDTO result = service.diagnose(request(10000, 30000, 0, 0, answers));
        assertThat(result.score()).isEqualTo(70);
        assertThat(result.factors()).extracting(Factor::code).containsExactly("T1_ADDRESS", "A2_RECENT_OWNER");
        assertThat(result.factors().get(0).explanation()).endsWith("중개사에게 확인하세요.");
        assertThat(result.factors().get(0).specialClause()).isNull();
    }

    @Test
    void mortgage_yesIsZeroPointsButCountsInDebtRatio() {
        Map<String, String> answers = safeAnswers();
        answers.put("B1_MORTGAGE", "YES");
        DiagnosisResponseDTO result = service.diagnose(request(10000, 20000, 5000, 0, answers));
        assertThat(result.debtRatio()).isEqualByComparingTo("75.0");
        assertThat(result.score()).isEqualTo(10);
        assertThat(result.factors()).extracting(Factor::code).containsExactly("DEBT_RATIO", "B1_MORTGAGE");
    }

    @Test
    void mortgage_notYes_ignoresAmount_unknownIs10() {
        Map<String, String> answers = safeAnswers();
        DiagnosisResponseDTO no = service.diagnose(request(10000, 20000, 5000, 0, answers));
        assertThat(no.debtRatio()).isEqualByComparingTo("50.0");
        assertThat(no.factors()).isEmpty();

        answers.put("B1_MORTGAGE", "UNKNOWN");
        DiagnosisResponseDTO unknown = service.diagnose(request(10000, 20000, 5000, 0, answers));
        assertThat(unknown.debtRatio()).isEqualByComparingTo("50.0");
        assertThat(unknown.score()).isEqualTo(10);
        assertThat(unknown.factors()).singleElement()
                .satisfies(f -> assertThat(f.title()).isEqualTo("근저당권 확인 필요"));
    }

    @Test
    void debtRatio_scoreUsesUnroundedValue() {
        assertThat(debt(27000, 30000)).isEqualTo(new Object[]{new BigDecimal("90.0"), 40});
        // 89.99%는 90.0으로 보이지만 점수는 80% 구간
        assertThat(debt(26997, 30000)).isEqualTo(new Object[]{new BigDecimal("90.0"), 25});
        assertThat(debt(24000, 30000)).isEqualTo(new Object[]{new BigDecimal("80.0"), 25});
        assertThat(debt(23997, 30000)).isEqualTo(new Object[]{new BigDecimal("80.0"), 10});
        assertThat(debt(21000, 30000)).isEqualTo(new Object[]{new BigDecimal("70.0"), 10});
        assertThat(debt(20997, 30000)).isEqualTo(new Object[]{new BigDecimal("70.0"), 0});
    }

    @Test
    void debtRatio_sumsMortgageSeniorAndDeposit() {
        Map<String, String> answers = safeAnswers();
        answers.put("B1_MORTGAGE", "YES");
        // (3000 + 2000 + 15000) / 20000 = 100%
        DiagnosisResponseDTO result = service.diagnose(request(15000, 20000, 3000, 2000, answers));
        assertThat(result.debtRatio()).isEqualByComparingTo("100.0");
        assertThat(result.factors().get(0).title()).isEqualTo("부채비율 100%");
    }

    @Test
    void invalidInput_hasField() {
        assertInvalid(request(0, 28000, 0, 0, safeAnswers()), "deposit", "보증금은 0보다 커야 해요");
        assertInvalid(request(null, 28000, 0, 0, safeAnswers()), "deposit", "보증금을 입력해 주세요");
        assertInvalid(request(1, 0, 0, 0, safeAnswers()), "salePrice", "매매 시세는 0보다 커야 해요");
        assertInvalid(request(1, 1, -1, 0, safeAnswers()), "mortgageMax", "채권최고액은 0 이상이어야 해요");
        assertInvalid(request(1, 1, 0, -1, safeAnswers()), "seniorDeposit", "선순위 보증금은 0 이상이어야 해요");

        Map<String, String> missing = safeAnswers();
        missing.remove("R1_VIOLATION");
        assertInvalid(request(1, 1, 0, 0, missing), "answers", "질문 10개에 모두 답해 주세요");
        Map<String, String> wrongAnswer = safeAnswers();
        wrongAnswer.put("T1_ADDRESS", "MAYBE");
        assertInvalid(request(1, 1, 0, 0, wrongAnswer), "answers", "답은 YES, NO, UNKNOWN 중 하나여야 해요");
        Map<String, String> nullAnswer = safeAnswers();
        nullAnswer.put("T1_ADDRESS", null);
        assertInvalid(request(1, 1, 0, 0, nullAnswer), "answers", "답은 YES, NO, UNKNOWN 중 하나여야 해요");
        Map<String, String> wrongCode = safeAnswers();
        wrongCode.put("X1_NOPE", "YES");
        assertInvalid(request(1, 1, 0, 0, wrongCode), "answers", "없는 질문 코드가 있어요");

        DiagnosisRequestDTO base = request(1, 1, 0, 0, safeAnswers());
        List<DiagnosisRequestDTO.Answer> duplicated = new ArrayList<>(base.answers());
        duplicated.add(new DiagnosisRequestDTO.Answer("T1_ADDRESS", "YES"));
        assertInvalid(new DiagnosisRequestDTO(1, 1, 0, 0, duplicated), "answers", "같은 질문에 두 번 답했어요");
        assertInvalid(new DiagnosisRequestDTO(1, 1, 0, 0, null), "answers", "질문 10개에 모두 답해 주세요");
    }

    @Test
    void ai_onlyReplacesExplanation() {
        AdviceService template = new AdviceService(new ClaudeAiExplainer("", "claude-opus-5-5"));
        AdviceService polished = new AdviceService((text, result, factors) -> Optional.of("쉽게 다듬은 설명이에요."));
        var score = service.calculate(caseB(0));

        AdviceResult fallback = template.advise(score);
        AdviceResult ai = polished.advise(score);
        assertThat(fallback.aiUsed()).isFalse();
        assertThat(fallback.aiExplanation()).startsWith("이 집은 위험 등급이에요.");
        assertThat(ai.aiUsed()).isTrue();
        assertThat(ai.aiExplanation()).isEqualTo("쉽게 다듬은 설명이에요.");
        assertThat(ai.factors()).isEqualTo(fallback.factors());
        assertThat(ai.gradeLabel()).isEqualTo(fallback.gradeLabel());
    }

    private Object[] debt(int deposit, int salePrice) {
        DiagnosisResponseDTO result = service.diagnose(request(deposit, salePrice, 0, 0, safeAnswers()));
        return new Object[]{result.debtRatio(), result.score()};
    }

    private void assertInvalid(DiagnosisRequestDTO request, String field, String message) {
        assertThatThrownBy(() -> service.diagnose(request))
                .isInstanceOfSatisfying(InvalidInputException.class, e -> {
                    assertThat(e.getField()).isEqualTo(field);
                    assertThat(e.getMessage()).isEqualTo(message);
                });
    }
}
