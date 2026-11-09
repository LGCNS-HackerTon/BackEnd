package com.lgcns.jeonse.domain.diagnosis.advice;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.OutputConfig;
import com.anthropic.models.messages.StopReason;
import com.lgcns.jeonse.domain.diagnosis.dto.DiagnosisResponseDTO;
import com.lgcns.jeonse.domain.diagnosis.dto.ScoreResult;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

// ANTHROPIC_API_KEY가 있을 때만 Claude로 설명 문단을 다듬는다. 점수·등급·항목은 입력으로만 주고 결과에 쓰지 않는다
@Component
public class ClaudeAiExplainer implements AiExplainer {
    private static final Logger log = LoggerFactory.getLogger(ClaudeAiExplainer.class);
    private static final String SYSTEM = """
            너는 처음 전세 계약을 하는 사회초년생에게 등기부 진단 결과를 설명하는 도우미야.
            아래 진단 결과와 기본 문단을 바탕으로, 쉬운 해요체 한국어 한 문단(3~4문장)으로 다시 써.
            - 등급, 위험 항목, 부채비율 숫자는 주어진 그대로 쓰고 새로 판단하거나 바꾸지 마.
            - 주어지지 않은 위험이나 사실을 지어내지 마. 법률 용어는 쉬운 말로 풀어.
            - 안전하다고 보장하거나 법적 판단처럼 단정하지 마.
            - 설명 문단만 출력해. 제목, 목록, 따옴표로 감싸기는 하지 마.""";

    private final AnthropicClient client;
    private final String model;

    public ClaudeAiExplainer(@Value("${anthropic.api-key:}") String apiKey,
                             @Value("${anthropic.model:claude-opus-5-5}") String model) {
        // AI-02: 5초 안에 답이 없으면 템플릿. 재시도하면 5초를 넘기므로 끈다
        this.client = apiKey.isBlank() ? null : AnthropicOkHttpClient.builder()
                .apiKey(apiKey).timeout(Duration.ofSeconds(5)).maxRetries(0).build();
        this.model = model;
    }

    @Override
    public Optional<String> polish(String template, ScoreResult result, List<DiagnosisResponseDTO.Factor> factors) {
        if (client == null) return Optional.empty();
        try {
            MessageCreateParams params = MessageCreateParams.builder()
                    .model(model)
                    .maxTokens(2000L)
                    .outputConfig(OutputConfig.builder().effort(OutputConfig.Effort.LOW).build())
                    .system(SYSTEM)
                    .addUserMessage(prompt(template, result, factors))
                    .build();
            Message message = client.messages().create(params);
            if (message.stopReason().filter(StopReason.END_TURN::equals).isEmpty()) return Optional.empty();
            String text = message.content().stream()
                    .flatMap(block -> block.text().stream())
                    .map(textBlock -> textBlock.text())
                    .collect(Collectors.joining()).strip();
            return text.isEmpty() ? Optional.empty() : Optional.of(text);
        } catch (RuntimeException e) {
            log.warn("AI 설명 생성 실패, 템플릿 사용: {}", e.getClass().getSimpleName());
            return Optional.empty();
        }
    }

    private static String prompt(String template, ScoreResult result, List<DiagnosisResponseDTO.Factor> factors) {
        String items = factors.isEmpty() ? "없음" : factors.stream()
                .map(f -> "- " + f.title() + (f.score() > 0 ? "" : " (안내용)"))
                .collect(Collectors.joining("\n"));
        return "등급: " + AdviceService.label(result.grade()) + "\n부채비율: " + result.debtRatio().toPlainString() + "%\n위험 항목:\n" + items
                + "\n\n기본 문단:\n" + template;
    }
}
