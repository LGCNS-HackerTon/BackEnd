package com.lgcns.jeonse.domain.question.dto;

import java.util.List;

public record QuestionResponseDTO(List<QuestionItem> questions) {
    public record QuestionItem(String code, String section, String sectionName, int order,
                               String text, String help, boolean needsAmount) { }
}
