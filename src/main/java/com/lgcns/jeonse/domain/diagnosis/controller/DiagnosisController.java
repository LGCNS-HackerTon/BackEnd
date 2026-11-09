package com.lgcns.jeonse.domain.diagnosis.controller;

import com.lgcns.jeonse.domain.diagnosis.dto.DiagnosisRequestDTO;
import com.lgcns.jeonse.domain.diagnosis.dto.DiagnosisResponseDTO;
import com.lgcns.jeonse.domain.diagnosis.service.DiagnosisService;
import com.lgcns.jeonse.global.security.LoginMember;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/diagnoses")
public class DiagnosisController {
    private final DiagnosisService service;
    public DiagnosisController(DiagnosisService service) { this.service = service; }

    // 토큰 선택: 없으면 비회원(저장 안 함), 있으면 회원 기록으로 저장, 잘못됐으면 401 (docs/03 6.1)
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DiagnosisResponseDTO diagnose(@LoginMember(required = false) Long memberId,
                                         @RequestBody DiagnosisRequestDTO request) {
        return service.diagnose(request, memberId);
    }
}
