package com.lgcns.jeonse.domain.diagnosis.dto;

import java.util.List;

// 금액은 만원 단위. seniorDeposit은 null과 0을 구분해야 해서 Integer로 받는다 (docs/03 2절).
public record DiagnosisRequestDTO(Integer deposit, Integer salePrice, Integer mortgageMax, Integer seniorDeposit,
                                  List<Answer> answers) {
    public record Answer(String code, String answer) { }
}
