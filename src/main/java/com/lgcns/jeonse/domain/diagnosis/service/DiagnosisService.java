package com.lgcns.jeonse.domain.diagnosis.service;

import com.lgcns.jeonse.domain.diagnosis.advice.AdviceResult;
import com.lgcns.jeonse.domain.diagnosis.advice.AdviceService;
import com.lgcns.jeonse.domain.diagnosis.dto.DiagnosisRequestDTO;
import com.lgcns.jeonse.domain.diagnosis.dto.DiagnosisResponseDTO;
import com.lgcns.jeonse.domain.diagnosis.dto.ScoreResult;
import com.lgcns.jeonse.domain.diagnosis.entity.RiskGrade;
import com.lgcns.jeonse.domain.question.entity.RegistryQuestion;
import com.lgcns.jeonse.domain.question.repository.RegistryQuestionRepository;
import com.lgcns.jeonse.global.exception.InvalidInputException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class DiagnosisService {
    public static final String MORTGAGE = "B1_MORTGAGE";
    private static final Set<String> ANSWERS = Set.of("YES", "NO", "UNKNOWN");

    private final RegistryQuestionRepository questionRepository;
    private final AdviceService adviceService;

    public DiagnosisService(RegistryQuestionRepository questionRepository, AdviceService adviceService) {
        this.questionRepository = questionRepository;
        this.adviceService = adviceService;
    }

    // 비로그인 진단: 저장하지 않으므로 id는 null, createdAt은 응답 시각 (docs/03 6절)
    public DiagnosisResponseDTO diagnose(DiagnosisRequestDTO request) {
        ScoreResult result = calculate(request);
        AdviceResult advice = adviceService.advise(result);
        return new DiagnosisResponseDTO(null, result.score(), result.grade().name(), advice.gradeLabel(),
                advice.gradeMessage(), result.debtRatio(), advice.factors(), advice.commonAdvice(),
                advice.aiExplanation(), advice.aiUsed(), advice.warnings(), advice.disclaimer(),
                LocalDateTime.now().withNano(0));
    }

    // 규칙 기반 점수 계산 (docs/01 1~3절). 같은 입력은 항상 같은 결과
    public ScoreResult calculate(DiagnosisRequestDTO request) {
        int deposit = positive(request.deposit(), "deposit", "보증금을", "보증금은");
        int salePrice = positive(request.salePrice(), "salePrice", "매매 시세를", "매매 시세는");
        int mortgageInput = notNegative(request.mortgageMax(), "mortgageMax", "채권최고액은");
        int seniorDeposit = notNegative(request.seniorDeposit(), "seniorDeposit", "선순위 보증금은");
        List<RegistryQuestion> questions = questionRepository.findAllByOrderBySortOrderAsc();
        Map<String, String> answers = validAnswers(request.answers(), questions);

        List<ScoreResult.ScoredAnswer> scored = new ArrayList<>();
        int score = 0;
        for (RegistryQuestion question : questions) {
            String answer = answers.get(question.getCode());
            int points = answerScore(question, answer);
            scored.add(new ScoreResult.ScoredAnswer(question, answer, points));
            score += points;
        }

        int mortgageMax = "YES".equals(answers.get(MORTGAGE)) ? mortgageInput : 0;
        long totalDebt = (long) mortgageMax + seniorDeposit + deposit;
        BigDecimal debtRatio = BigDecimal.valueOf(totalDebt * 100)
                .divide(BigDecimal.valueOf(salePrice), 1, RoundingMode.HALF_UP);
        int debtRatioScore = debtRatioScore(totalDebt, salePrice);
        score += debtRatioScore;

        return new ScoreResult(score, RiskGrade.of(score), debtRatio, debtRatioScore, scored, mortgageMax,
                request.seniorDeposit() == null);
    }

    // UNKNOWN은 위험한 답으로 계산. 근저당 YES는 금액이 부채비율에 들어가므로 질문 자체는 0점
    static int answerScore(RegistryQuestion question, String answer) {
        if ("UNKNOWN".equals(answer)) return question.getRiskScore();
        if (MORTGAGE.equals(question.getCode())) return 0;
        return answer.equals(question.getRiskyAnswer()) ? question.getRiskScore() : 0;
    }

    // 구간 판정은 반올림 전 값으로: 정수 곱셈으로 비교해 소수 오차를 없앤다
    static int debtRatioScore(long totalDebt, int salePrice) {
        long percent = totalDebt * 100;
        if (percent >= 90L * salePrice) return 40;
        if (percent >= 80L * salePrice) return 25;
        if (percent >= 70L * salePrice) return 10;
        return 0;
    }

    private static Map<String, String> validAnswers(List<DiagnosisRequestDTO.Answer> answers,
                                                    List<RegistryQuestion> questions) {
        if (answers == null) throw new InvalidInputException("질문 10개에 모두 답해 주세요", "answers");
        Set<String> codes = questions.stream().map(RegistryQuestion::getCode).collect(Collectors.toSet());
        Map<String, String> result = new HashMap<>();
        for (DiagnosisRequestDTO.Answer answer : answers) {
            if (answer == null || answer.code() == null || !codes.contains(answer.code()))
                throw new InvalidInputException("없는 질문 코드가 있어요", "answers");
            if (!ANSWERS.contains(answer.answer()))
                throw new InvalidInputException("답은 YES, NO, UNKNOWN 중 하나여야 해요", "answers");
            if (result.put(answer.code(), answer.answer()) != null)
                throw new InvalidInputException("같은 질문에 두 번 답했어요", "answers");
        }
        if (result.size() != codes.size()) throw new InvalidInputException("질문 10개에 모두 답해 주세요", "answers");
        return result;
    }

    private static int positive(Integer value, String field, String object, String subject) {
        if (value == null) throw new InvalidInputException(object + " 입력해 주세요", field);
        if (value < 1) throw new InvalidInputException(subject + " 0보다 커야 해요", field);
        return value;
    }

    private static int notNegative(Integer value, String field, String subject) {
        if (value == null) return 0;
        if (value < 0) throw new InvalidInputException(subject + " 0 이상이어야 해요", field);
        return value;
    }
}
