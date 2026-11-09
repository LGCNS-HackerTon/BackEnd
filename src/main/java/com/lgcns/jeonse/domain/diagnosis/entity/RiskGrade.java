package com.lgcns.jeonse.domain.diagnosis.entity;

// 경계를 바꾸면 db/schema.sql의 ck_diag_grade도 같은 PR에서 바꾼다 (docs/04)
public enum RiskGrade {
    SAFE, CAUTION, DANGER;

    public static RiskGrade of(int score) {
        if (score >= 60) return DANGER;
        if (score >= 30) return CAUTION;
        return SAFE;
    }
}
