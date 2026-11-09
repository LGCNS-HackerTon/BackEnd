package com.lgcns.jeonse.question.dto;

import java.util.List;

public record QuestionResponse(List<QuestionItem> questions) {
    public record QuestionItem(String code, String section, String sectionName, int order,
                               String text, String help, boolean needsAmount) { }
}
