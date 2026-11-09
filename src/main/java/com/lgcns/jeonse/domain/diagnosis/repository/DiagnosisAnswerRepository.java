package com.lgcns.jeonse.domain.diagnosis.repository;

import com.lgcns.jeonse.domain.diagnosis.entity.DiagnosisAnswer;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DiagnosisAnswerRepository extends JpaRepository<DiagnosisAnswer, Long> {
    // 답은 질문 order 순서로 저장하고 재평가 때도 행을 그대로 두므로 id 순서 = 질문 순서
    List<DiagnosisAnswer> findByDiagnosisIdInOrderByIdAsc(Collection<Long> diagnosisIds);
}
