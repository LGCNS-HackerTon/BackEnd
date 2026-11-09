package com.lgcns.jeonse.domain.diagnosis.repository;

import com.lgcns.jeonse.domain.diagnosis.entity.Diagnosis;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DiagnosisRepository extends JpaRepository<Diagnosis, Long> {
    // 최근 5건 (idx_diag_user_created). 같은 초에 저장된 건은 id로 순서를 정한다
    List<Diagnosis> findTop5ByUserIdOrderByCreatedAtDescIdDesc(Long userId);

    Optional<Diagnosis> findByIdAndUserId(Long id, Long userId);
}
