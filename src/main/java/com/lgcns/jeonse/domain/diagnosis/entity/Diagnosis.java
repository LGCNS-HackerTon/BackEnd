package com.lgcns.jeonse.domain.diagnosis.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

// 로그인 회원 진단만 저장한다 (docs/03 6절). 컬럼은 db/schema.sql diagnoses와 같다
@Entity
@Table(name = "diagnoses")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Diagnosis {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "user_id") private Long userId;
    @Column(name = "property_name", length = 100) private String propertyName;
    @Column(nullable = false) private int deposit;
    @Column(name = "sale_price", nullable = false) private int salePrice;
    @Column(name = "mortgage_max", nullable = false) private int mortgageMax;
    @Column(name = "senior_deposit", nullable = false) private int seniorDeposit;
    @Column(name = "senior_unknown", nullable = false) private boolean seniorUnknown;
    @Column(name = "debt_ratio", nullable = false, precision = 6, scale = 1) private BigDecimal debtRatio;
    @Column(name = "risk_score", nullable = false) private int riskScore;
    @Enumerated(EnumType.STRING)
    @Column(name = "risk_grade", nullable = false, columnDefinition = "VARCHAR(10)") private RiskGrade riskGrade;
    @Column(name = "ai_explanation", nullable = false, columnDefinition = "TEXT") private String aiExplanation;
    @Column(name = "ai_used", nullable = false) private boolean aiUsed;
    @CreationTimestamp @Column(name = "created_at", nullable = false, updatable = false) private LocalDateTime createdAt;
    @UpdateTimestamp @Column(name = "updated_at", nullable = false) private LocalDateTime updatedAt;

    public Diagnosis(Long userId, String propertyName, int deposit, int salePrice, int mortgageMax, int seniorDeposit,
                     boolean seniorUnknown, BigDecimal debtRatio, int riskScore, RiskGrade riskGrade,
                     String aiExplanation, boolean aiUsed) {
        this.userId = userId; this.propertyName = propertyName; this.deposit = deposit; this.salePrice = salePrice;
        this.mortgageMax = mortgageMax; this.seniorDeposit = seniorDeposit; this.seniorUnknown = seniorUnknown;
        this.debtRatio = debtRatio; this.riskScore = riskScore; this.riskGrade = riskGrade;
        this.aiExplanation = aiExplanation; this.aiUsed = aiUsed;
    }
}
