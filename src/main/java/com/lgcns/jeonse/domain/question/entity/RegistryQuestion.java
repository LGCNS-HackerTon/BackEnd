package com.lgcns.jeonse.domain.question.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "registry_questions")
@Getter
@NoArgsConstructor
public class RegistryQuestion {
    @Id private String code;
    @Column(nullable = false) private String section;
    @Column(name = "sort_order", nullable = false) private int sortOrder;
    @Column(nullable = false) private String text;
    @Column(nullable = false, columnDefinition = "TEXT") private String help;
    @Column(name = "risky_answer", nullable = false) private String riskyAnswer;
    @Column(name = "risk_score", nullable = false) private int riskScore;
    @Column(name = "needs_amount", nullable = false) private boolean needsAmount;
    @Column(name = "factor_title", nullable = false) private String factorTitle;
    @Column(name = "ask_agent", nullable = false) private String askAgent;
    @Column(name = "special_clause", columnDefinition = "TEXT") private String specialClause;

    public RegistryQuestion(String code, String section, int sortOrder, String text, String help, String riskyAnswer,
                            int riskScore, boolean needsAmount, String factorTitle, String askAgent, String specialClause) {
        this.code = code; this.section = section; this.sortOrder = sortOrder;
        this.text = text; this.help = help; this.riskyAnswer = riskyAnswer; this.riskScore = riskScore;
        this.needsAmount = needsAmount; this.factorTitle = factorTitle; this.askAgent = askAgent;
        this.specialClause = specialClause;
    }
}
