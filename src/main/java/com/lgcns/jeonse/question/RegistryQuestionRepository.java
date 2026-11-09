package com.lgcns.jeonse.question;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RegistryQuestionRepository extends JpaRepository<RegistryQuestion, String> {
    List<RegistryQuestion> findAllByOrderBySortOrderAsc();
}
