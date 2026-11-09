package com.lgcns.jeonse.domain.diagnosis.service;

import com.lgcns.jeonse.domain.diagnosis.advice.AdviceResult;
import com.lgcns.jeonse.domain.diagnosis.advice.AdviceService;
import com.lgcns.jeonse.domain.diagnosis.dto.DiagnosisHistoryResponseDTO;
import com.lgcns.jeonse.domain.diagnosis.dto.DiagnosisRequestDTO;
import com.lgcns.jeonse.domain.diagnosis.dto.ScoreResult;
import com.lgcns.jeonse.domain.diagnosis.entity.Diagnosis;
import com.lgcns.jeonse.domain.diagnosis.entity.DiagnosisAnswer;
import com.lgcns.jeonse.domain.diagnosis.repository.DiagnosisAnswerRepository;
import com.lgcns.jeonse.domain.diagnosis.repository.DiagnosisRepository;
import com.lgcns.jeonse.global.exception.ApiException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// 로그인 회원 진단 기록 저장·조회·덮어쓰기 (docs/03 6장 2·9·10번).
// 점수 계산·AI 설명은 DiagnosisService가 트랜잭션 밖에서 끝내고, 여기서는 DB 쓰기만 짧게 한다
@Service
public class DiagnosisHistoryService {
    private final DiagnosisRepository diagnosisRepository;
    private final DiagnosisAnswerRepository answerRepository;

    public DiagnosisHistoryService(DiagnosisRepository diagnosisRepository, DiagnosisAnswerRepository answerRepository) {
        this.diagnosisRepository = diagnosisRepository;
        this.answerRepository = answerRepository;
    }

    @Transactional
    public Diagnosis save(long memberId, DiagnosisRequestDTO request, ScoreResult result, AdviceResult advice) {
        Diagnosis diagnosis = diagnosisRepository.save(new Diagnosis(memberId, location(request), request.deposit(),
                request.salePrice(), result.mortgageMax(), seniorDeposit(request), result.seniorDepositMissing(),
                result.debtRatio(), result.score(), result.grade(), advice.aiExplanation(), advice.aiUsed()));
        answerRepository.saveAll(result.scoredAnswers().stream()
                .map(scored -> new DiagnosisAnswer(diagnosis, scored.code(), scored.answer(), scored.score())).toList());
        return diagnosis;
    }

    // 같은 행(진단 1건 + 답 10개)에 덮어쓴다. 새 행을 만들지 않는다
    @Transactional
    public Diagnosis overwrite(long memberId, long id, DiagnosisRequestDTO request, ScoreResult result,
                               AdviceResult advice) {
        Diagnosis diagnosis = owned(memberId, id);
        diagnosis.overwrite(location(request), request.deposit(), request.salePrice(), result.mortgageMax(),
                seniorDeposit(request), result.seniorDepositMissing(), result.debtRatio(), result.score(),
                result.grade(), advice.aiExplanation(), advice.aiUsed());
        Map<String, DiagnosisAnswer> answers = answerRepository.findByDiagnosisIdInOrderByIdAsc(List.of(id)).stream()
                .collect(Collectors.toMap(DiagnosisAnswer::getQuestionCode, Function.identity()));
        for (ScoreResult.ScoredAnswer scored : result.scoredAnswers())
            answers.get(scored.code()).overwrite(scored.answer(), scored.score());
        return diagnosis;
    }

    // 남의 기록이 있는지도 알려주지 않게 없는 id와 같은 404
    @Transactional(readOnly = true)
    public Diagnosis owned(long memberId, long id) {
        return diagnosisRepository.findByIdAndUserId(id, memberId).orElseThrow(() ->
                new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", "진단 기록을 찾을 수 없어요.", null));
    }

    // 가장 최근 5건을 골라 createdAt 오름차순으로 돌려준다
    @Transactional(readOnly = true)
    public DiagnosisHistoryResponseDTO recent(long memberId) {
        List<Diagnosis> diagnoses = new ArrayList<>(diagnosisRepository.findTop5ByUserIdOrderByCreatedAtDescIdDesc(memberId));
        Collections.reverse(diagnoses);
        Map<Long, List<DiagnosisRequestDTO.Answer>> answers = answerRepository
                .findByDiagnosisIdInOrderByIdAsc(diagnoses.stream().map(Diagnosis::getId).toList()).stream()
                .collect(Collectors.groupingBy(answer -> answer.getDiagnosis().getId(), Collectors.mapping(
                        answer -> new DiagnosisRequestDTO.Answer(answer.getQuestionCode(), answer.getAnswer()),
                        Collectors.toList())));
        return new DiagnosisHistoryResponseDTO(diagnoses.stream().map(d -> new DiagnosisHistoryResponseDTO.Item(
                d.getId(), d.getPropertyName(), d.getCreatedAt(), d.getReevaluatedAt(), d.getRiskScore(),
                d.getRiskGrade().name(), AdviceService.label(d.getRiskGrade()),
                new DiagnosisRequestDTO(d.getDeposit(), d.getSalePrice(), d.getMortgageMax(),
                        d.isSeniorUnknown() ? null : d.getSeniorDeposit(), answers.getOrDefault(d.getId(), List.of()))))
                .toList());
    }

    // FE src/services/history.js와 같은 문구: "보증금 2억 4,000만 원 · 시세 2억 8,000만 원"
    static String location(DiagnosisRequestDTO request) {
        return "보증금 " + money(request.deposit()) + " · 시세 " + money(request.salePrice());
    }

    // FE formatMoney와 같다: 만원 단위 정수 → "2억 4,000만 원", "2억 원", "5,000만 원"
    static String money(int man) {
        int eok = man / 10000, rest = man % 10000;
        List<String> parts = new ArrayList<>();
        if (eok != 0) parts.add(eok + "억");
        if (rest != 0 || eok == 0) parts.add(String.format(Locale.US, "%,d만", rest));
        return String.join(" ", parts) + " 원";
    }

    private static int seniorDeposit(DiagnosisRequestDTO request) {
        return request.seniorDeposit() == null ? 0 : request.seniorDeposit();
    }
}
