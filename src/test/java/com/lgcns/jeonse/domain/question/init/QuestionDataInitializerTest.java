package com.lgcns.jeonse.domain.question.init;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.lgcns.jeonse.domain.question.entity.RegistryQuestion;
import com.lgcns.jeonse.domain.question.repository.RegistryQuestionRepository;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class QuestionDataInitializerTest {
    private final RegistryQuestionRepository repository = mock(RegistryQuestionRepository.class);

    @SuppressWarnings("unchecked")
    private List<String> savedCodes() {
        ArgumentCaptor<List<RegistryQuestion>> saved = ArgumentCaptor.forClass(List.class);
        verify(repository).saveAll(saved.capture());
        return saved.getValue().stream().map(RegistryQuestion::getCode).toList();
    }

    @Test
    void 빈_테이블이면_10개를_모두_넣는다() {
        when(repository.findAll()).thenReturn(List.of());

        assertThat(QuestionDataInitializer.fillMissing(repository)).isEqualTo(10);
        assertThat(savedCodes()).hasSize(10);
    }

    @Test
    void 일부만_있으면_빠진_질문만_넣고_있는_행은_건드리지_않는다() {
        List<RegistryQuestion> existing = QuestionDataInitializer.defaults().stream()
                .filter(q -> !q.getCode().equals("B2_LEASE_REG") && !q.getCode().equals("R1_VIOLATION")).toList();
        when(repository.findAll()).thenReturn(existing);

        assertThat(QuestionDataInitializer.fillMissing(repository)).isEqualTo(2);
        assertThat(savedCodes()).containsExactly("B2_LEASE_REG", "R1_VIOLATION");
        verify(repository, never()).deleteAll();
        verify(repository, never()).deleteAll(any());
        verify(repository, never()).save(any());
    }

    @Test
    void 모두_있으면_아무것도_넣지_않는다() {
        when(repository.findAll()).thenReturn(QuestionDataInitializer.defaults());

        assertThat(QuestionDataInitializer.fillMissing(repository)).isZero();
        verify(repository, never()).saveAll(any());
    }

    @Test
    void 빠진_질문의_순서를_다른_행이_쓰면_넣지_않는다() {
        List<RegistryQuestion> existing = new ArrayList<>(QuestionDataInitializer.defaults().subList(0, 9));
        existing.add(new RegistryQuestion("X9_UNKNOWN", "BUILDING", 10, "q", "h", "YES", 10, false, "t", "a", null));
        when(repository.findAll()).thenReturn(existing);

        assertThat(QuestionDataInitializer.fillMissing(repository)).isZero();
        verify(repository, never()).saveAll(any());
    }
}
