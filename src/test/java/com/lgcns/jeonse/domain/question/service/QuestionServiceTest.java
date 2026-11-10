package com.lgcns.jeonse.domain.question.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.lgcns.jeonse.domain.question.dto.QuestionResponseDTO;
import com.lgcns.jeonse.domain.question.entity.RegistryQuestion;
import com.lgcns.jeonse.domain.question.init.QuestionDataInitializer;
import com.lgcns.jeonse.domain.question.repository.RegistryQuestionRepository;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class QuestionServiceTest {
    private final RegistryQuestionRepository repository = mock(RegistryQuestionRepository.class);
    private final QuestionService service = new QuestionService(repository);

    @Test
    void 정해진_10개를_순서대로_돌려준다() {
        when(repository.findAllByOrderBySortOrderAsc()).thenReturn(QuestionDataInitializer.defaults());

        QuestionResponseDTO dto = service.getQuestions();

        assertThat(dto.questions()).hasSize(10);
        assertThat(dto.questions().get(0).sectionName()).isEqualTo("표제부");
        assertThat(dto.questions().get(6).needsAmount()).isTrue();
    }

    @Test
    void 질문이_빠지면_일부만_돌려주지_않는다() {
        List<RegistryQuestion> partial = QuestionDataInitializer.defaults().stream()
                .filter(q -> !q.getCode().equals("R1_VIOLATION")).toList();
        when(repository.findAllByOrderBySortOrderAsc()).thenReturn(partial);

        assertThatThrownBy(service::getQuestions).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void 개수가_10개여도_모르는_코드가_섞이면_돌려주지_않는다() {
        List<RegistryQuestion> wrong = new ArrayList<>(QuestionDataInitializer.defaults().subList(0, 9));
        wrong.add(new RegistryQuestion("X9_UNKNOWN", "BUILDING", 10, "q", "h", "YES", 10, false, "t", "a", null));
        when(repository.findAllByOrderBySortOrderAsc()).thenReturn(wrong);

        assertThatThrownBy(service::getQuestions).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void 코드가_맞아도_순서가_다르면_돌려주지_않는다() {
        List<RegistryQuestion> swapped = new ArrayList<>(QuestionDataInitializer.defaults().subList(0, 9));
        swapped.add(new RegistryQuestion("R1_VIOLATION", "BUILDING", 11, "q", "h", "YES", 20, false, "t", "a", null));
        when(repository.findAllByOrderBySortOrderAsc()).thenReturn(swapped);

        assertThatThrownBy(service::getQuestions).isInstanceOf(IllegalStateException.class);
    }
}
