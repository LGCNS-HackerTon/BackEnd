package com.lgcns.jeonse.domain.diagnosis.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

// FrontEnd src/services/history.js formatMoney와 같은 결과인지 (같은 입력을 JS로 돌린 값)
class DiagnosisHistoryServiceTest {
    @Test
    void money_FE_formatMoney와_같다() {
        assertThat(DiagnosisHistoryService.money(24000)).isEqualTo("2억 4,000만 원");
        assertThat(DiagnosisHistoryService.money(28000)).isEqualTo("2억 8,000만 원");
        assertThat(DiagnosisHistoryService.money(20000)).isEqualTo("2억 원");
        assertThat(DiagnosisHistoryService.money(10000)).isEqualTo("1억 원");
        assertThat(DiagnosisHistoryService.money(5000)).isEqualTo("5,000만 원");
        assertThat(DiagnosisHistoryService.money(950)).isEqualTo("950만 원");
        assertThat(DiagnosisHistoryService.money(1)).isEqualTo("1만 원");
        assertThat(DiagnosisHistoryService.money(123456)).isEqualTo("12억 3,456만 원");
        assertThat(DiagnosisHistoryService.money(10001)).isEqualTo("1억 1만 원");
    }
}
