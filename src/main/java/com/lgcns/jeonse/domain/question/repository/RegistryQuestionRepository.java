package com.lgcns.jeonse.domain.question.repository;

import com.lgcns.jeonse.domain.question.entity.RegistryQuestion;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RegistryQuestionRepository extends JpaRepository<RegistryQuestion, String> {
    List<RegistryQuestion> findAllByOrderBySortOrderAsc();
}
