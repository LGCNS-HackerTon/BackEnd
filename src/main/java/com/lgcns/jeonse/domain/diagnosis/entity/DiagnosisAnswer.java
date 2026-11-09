package com.lgcns.jeonse.domain.diagnosis.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 컬럼은 db/schema.sql diagnosis_answers와 같다
@Entity
@Table(name = "diagnosis_answers",
        uniqueConstraints = @UniqueConstraint(name = "uk_diag_question", columnNames = {"diagnosis_id", "question_code"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DiagnosisAnswer {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "diagnosis_id", nullable = false) private Diagnosis diagnosis;
    @Column(name = "question_code", nullable = false, length = 30) private String questionCode;
    @Column(nullable = false, length = 10) private String answer;
    @Column(nullable = false) private int score;

    public DiagnosisAnswer(Diagnosis diagnosis, String questionCode, String answer, int score) {
        this.diagnosis = diagnosis; this.questionCode = questionCode; this.answer = answer; this.score = score;
    }
}
