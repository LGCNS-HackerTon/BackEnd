package com.lgcns.jeonse.domain.diagnosis;

import static org.assertj.core.api.Assertions.assertThat;

import com.lgcns.jeonse.domain.diagnosis.entity.Diagnosis;
import com.lgcns.jeonse.domain.diagnosis.entity.DiagnosisAnswer;
import com.lgcns.jeonse.domain.diagnosis.entity.RiskGrade;
import com.lgcns.jeonse.domain.diagnosis.repository.DiagnosisAnswerRepository;
import com.lgcns.jeonse.domain.diagnosis.repository.DiagnosisRepository;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

@DataJpaTest
class DiagnosisRepositoryTest {
    @Autowired DiagnosisRepository diagnoses;
    @Autowired DiagnosisAnswerRepository answers;

    @Test
    void savesDiagnosisWithTimestampsAndAnswer() {
        Diagnosis saved = diagnoses.saveAndFlush(new Diagnosis(null, null, 24000, 28000, 3600, 0, false,
                new BigDecimal("98.6"), 70, RiskGrade.DANGER, "설명", false));
        DiagnosisAnswer answer = answers.saveAndFlush(new DiagnosisAnswer(saved, "T2_USAGE", "NO", 20));

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
        assertThat(answer.getDiagnosis().getId()).isEqualTo(saved.getId());
    }
}
