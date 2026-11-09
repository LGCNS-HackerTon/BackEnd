package com.lgcns.jeonse.question;

import com.lgcns.jeonse.question.dto.QuestionResponse;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class QuestionService {
    private static final Map<String, String> SECTION_NAMES = Map.of(
            "TITLE", "표제부", "GAPGU", "갑구", "EULGU", "을구", "BUILDING", "건축물대장");
    private final RegistryQuestionRepository repository;

    public QuestionService(RegistryQuestionRepository repository) { this.repository = repository; }

    public QuestionResponse getQuestions() {
        List<QuestionResponse.QuestionItem> items = repository.findAllByOrderBySortOrderAsc().stream()
                .map(q -> new QuestionResponse.QuestionItem(q.getCode(), q.getSection(),
                        SECTION_NAMES.get(q.getSection()), q.getSortOrder(), q.getText(), q.getHelp(), q.isNeedsAmount()))
                .toList();
        return new QuestionResponse(items);
    }
}
