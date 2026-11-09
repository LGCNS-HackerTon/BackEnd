package com.lgcns.jeonse.domain.diagnosis.repository;

import com.lgcns.jeonse.domain.diagnosis.entity.Diagnosis;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DiagnosisRepository extends JpaRepository<Diagnosis, Long> { }
