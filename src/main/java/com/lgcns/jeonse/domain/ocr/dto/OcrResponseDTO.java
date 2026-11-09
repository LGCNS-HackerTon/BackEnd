package com.lgcns.jeonse.domain.ocr.dto;

import java.util.List;

public record OcrResponseDTO(AddressMatch addressMatch, List<Answer> answers, Integer mortgageMax,
                             HousingType housingType, List<String> warnings) {
    public record Answer(String code, Status status, String answer, String evidence) { }

    public enum Status { CONFIRMED, UNCONFIRMED }

    public enum AddressMatch { MATCH, MISMATCH, UNKNOWN }

    public enum HousingType { APARTMENT, MULTIPLEX, MULTI_HOUSEHOLD, DETACHED, OFFICETEL, NON_RESIDENTIAL }
}
