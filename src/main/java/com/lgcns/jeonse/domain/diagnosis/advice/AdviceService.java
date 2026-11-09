package com.lgcns.jeonse.domain.diagnosis.advice;

import com.lgcns.jeonse.domain.diagnosis.dto.DiagnosisResponseDTO.CommonAdvice;
import com.lgcns.jeonse.domain.diagnosis.dto.DiagnosisResponseDTO.Factor;
import com.lgcns.jeonse.domain.diagnosis.dto.ScoreResult;
import com.lgcns.jeonse.domain.diagnosis.entity.RiskGrade;
import com.lgcns.jeonse.domain.question.entity.RegistryQuestion;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;

// 결과 문구. 원본은 docs/01 2~4절과 docs/03 2절 예시. 점수·등급·factors 목록은 바꾸지 않는다
@Service
public class AdviceService {
    static final String DISCLAIMER = "참고용이며 법적 판단이 아닙니다. 계약 전 공인중개사·전문가에게 확인하세요.";
    static final String SENIOR_WARNING = "선순위 보증금을 모르면 0으로 계산했어요. 다가구주택이라면 중개사에게 꼭 물어보세요.";
    static final CommonAdvice COMMON = new CommonAdvice("국세·지방세 체납은 없으신가요?",
            "임대인은 잔금일 다음 날까지 등기부상 권리관계를 현재 상태로 유지하며, 근저당권 등 새로운 권리를 설정하지 않는다. "
                    + "위반 시 임차인은 계약을 해제할 수 있고 임대인은 손해를 배상한다.");
    private static final String DEBT_ASK = "이 집 매매 시세가 얼마인지, 근거(실거래가)를 보여주실 수 있나요?";
    private static final String DEBT_CLAUSE = "전세보증금반환보증 가입이 거절될 경우 본 계약은 무효로 하며, 임대인은 받은 계약금을 즉시 반환한다.";
    private static final String UNKNOWN_NOTE = " 모르는 답은 위험한 쪽으로 계산했어요. 중개사에게 확인하세요.";

    private final AiExplainer aiExplainer;

    public AdviceService(AiExplainer aiExplainer) { this.aiExplainer = aiExplainer; }

    public AdviceResult advise(ScoreResult result) {
        List<Factor> factors = factors(result);
        String template = template(result, factors);
        String polished = aiExplainer.polish(template, result, factors).orElse(null);
        String gradeMessage = switch (result.grade()) {
            case SAFE -> "큰 위험 신호는 없어요. 그래도 잔금일에 등기부를 한 번 더 확인하세요.";
            case CAUTION -> "확인이 필요한 항목이 있어요. 아래 질문을 중개사에게 꼭 물어보세요.";
            case DANGER -> "계약을 다시 생각해 보세요. 전문가 상담을 권해요.";
        };
        return new AdviceResult(label(result.grade()), gradeMessage, factors, COMMON, polished != null ? polished : template,
                polished != null, result.seniorDepositMissing() ? List.of(SENIOR_WARNING) : List.of(), DISCLAIMER);
    }

    // 점수 > 0인 항목을 점수 큰 순서로. 같은 점수는 질문 순서 → 부채비율. 근저당 YES는 0점이어도 마지막 (RS-02)
    static List<Factor> factors(ScoreResult result) {
        List<Factor> factors = new ArrayList<>();
        Factor mortgage = null;
        for (ScoreResult.ScoredAnswer scored : result.scoredAnswers()) {
            RegistryQuestion q = scored.question();
            boolean mortgageYes = "B1_MORTGAGE".equals(scored.code()) && "YES".equals(scored.answer());
            if (mortgageYes) {
                mortgage = new Factor(q.getCode(), 0, String.format("근저당권 있음 (채권최고액 %,d만 원)", result.mortgageMax()),
                        "이 집을 담보로 빌린 돈이 있어요. 금액은 부채비율에 반영했어요.", q.getAskAgent(), q.getSpecialClause());
            } else if (scored.score() > 0) {
                boolean unknown = "UNKNOWN".equals(scored.answer());
                // 근저당을 모르면 "있음"이라 단정하지 않는다
                String title = unknown && "B1_MORTGAGE".equals(scored.code()) ? "근저당권 확인 필요" : q.getFactorTitle();
                factors.add(new Factor(q.getCode(), scored.score(), title,
                        unknown ? q.getHelp() + UNKNOWN_NOTE : q.getHelp(), q.getAskAgent(), q.getSpecialClause()));
            }
        }
        if (result.debtRatioScore() > 0) {
            factors.add(new Factor("DEBT_RATIO", result.debtRatioScore(),
                    "부채비율 " + result.debtRatio().stripTrailingZeros().toPlainString() + "%",
                    debtExplanation(result.debtRatioScore()), DEBT_ASK, DEBT_CLAUSE));
        }
        factors.sort(Comparator.comparingInt(Factor::score).reversed());
        if (mortgage != null) factors.add(mortgage);
        return factors;
    }

    private static String debtExplanation(int score) {
        return switch (score) {
            case 40 -> "집을 팔아도 내 보증금을 다 못 돌려받을 수 있는 \"깡통전세\" 수준이에요.";
            case 25 -> "집값이 조금만 떨어져도 위험해요.";
            default -> "여유가 크지 않아요.";
        };
    }

    static String label(RiskGrade grade) {
        return switch (grade) { case SAFE -> "안전"; case CAUTION -> "주의"; case DANGER -> "위험"; };
    }

    // AI 미연동·실패 시 그대로 쓰는 한 문단 (RS-07)
    static String template(ScoreResult result, List<Factor> factors) {
        String label = label(result.grade());
        List<Factor> risky = factors.stream().filter(f -> f.score() > 0).toList();
        if (risky.isEmpty()) {
            return "이 집은 " + label + " 등급이에요. 입력한 답에서 큰 위험 신호는 발견되지 않았어요. "
                    + "그래도 안전을 보장하는 건 아니니 잔금일에 등기부를 한 번 더 확인하세요.";
        }
        return "이 집은 " + label + " 등급이에요. 확인이 필요한 항목은 " + risky.size() + "개이고, 가장 큰 이유는 '"
                + risky.get(0).title() + "' 항목이에요. 아래 항목별 질문을 중개사에게 물어보고, 특약 문구를 계약서에 넣을 수 있는지 확인하세요.";
    }
}
