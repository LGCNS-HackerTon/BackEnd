package com.lgcns.jeonse.domain.question.service;

import com.lgcns.jeonse.domain.question.dto.QuestionResponseDTO;
import com.lgcns.jeonse.domain.question.entity.RegistryQuestion;
import com.lgcns.jeonse.domain.question.init.QuestionDataInitializer;
import com.lgcns.jeonse.domain.question.repository.RegistryQuestionRepository;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class QuestionService {
    private static final Logger log = LoggerFactory.getLogger(QuestionService.class);
    private static final Map<String, String> SECTION_NAMES = Map.of(
            "TITLE", "표제부", "GAPGU", "갑구", "EULGU", "을구", "BUILDING", "건축물대장");
    // 정해진 질문 10개의 코드-순서 (docs/01)
    private static final Map<String, Integer> EXPECTED = QuestionDataInitializer.defaults().stream()
            .collect(Collectors.toUnmodifiableMap(RegistryQuestion::getCode, RegistryQuestion::getSortOrder));
    private final RegistryQuestionRepository repository;

    public QuestionService(RegistryQuestionRepository repository) { this.repository = repository; }

    public QuestionResponseDTO getQuestions() {
        List<RegistryQuestion> questions = repository.findAllByOrderBySortOrderAsc();
        checkComplete(questions);
        List<QuestionResponseDTO.QuestionItem> items = questions.stream()
                .map(q -> new QuestionResponseDTO.QuestionItem(q.getCode(), q.getSection(),
                        SECTION_NAMES.get(q.getSection()), q.getSortOrder(), q.getText(), q.getHelp(), q.isNeedsAmount()))
                .toList();
        return new QuestionResponseDTO(items);
    }

    /**
     * 질문이 정해진 10개(docs/01)와 코드·순서까지 똑같은지 확인한다.
     * 다르면 빠진 코드를 로그에 남기고 예외를 던져, 일부만 보여주지 않고 500 {@code INTERNAL_ERROR}로 알린다 (docs/03 1번).
     *
     * @throws IllegalStateException 질문 데이터가 정해진 10개와 다를 때
     */
    private static void checkComplete(List<RegistryQuestion> questions) {
        Map<String, Integer> actual = questions.stream().collect(Collectors.toMap(
                RegistryQuestion::getCode, RegistryQuestion::getSortOrder, (a, b) -> a));
        if (questions.size() == EXPECTED.size() && actual.equals(EXPECTED)) return;
        Set<String> missing = EXPECTED.keySet().stream().filter(code -> !actual.containsKey(code))
                .collect(Collectors.toCollection(TreeSet::new));
        log.error("질문 데이터가 정해진 10개와 달라요. 읽은 개수 {}, 빠진 코드 {}, 읽은 코드-순서 {}",
                questions.size(), missing, actual);
        throw new IllegalStateException("질문 데이터가 정해진 10개와 달라요.");
    }
}
