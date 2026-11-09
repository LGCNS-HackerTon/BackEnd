package com.lgcns.jeonse.domain.diagnosis.controller;

import com.lgcns.jeonse.domain.diagnosis.dto.DiagnosisHistoryResponseDTO;
import com.lgcns.jeonse.domain.diagnosis.dto.DiagnosisRequestDTO;
import com.lgcns.jeonse.domain.diagnosis.dto.DiagnosisResponseDTO;
import com.lgcns.jeonse.domain.diagnosis.service.DiagnosisHistoryService;
import com.lgcns.jeonse.domain.diagnosis.service.DiagnosisService;
import com.lgcns.jeonse.global.security.LoginMember;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// docs/03 9·10번: 회원 경로지만 진단 기록 로직이라 diagnosis 기능에 둔다 (담당 송석주)
@RestController
@RequestMapping("/api/members/me/diagnoses")
public class MemberDiagnosisController {
    private final DiagnosisService diagnosisService;
    private final DiagnosisHistoryService historyService;

    public MemberDiagnosisController(DiagnosisService diagnosisService, DiagnosisHistoryService historyService) {
        this.diagnosisService = diagnosisService;
        this.historyService = historyService;
    }

    @GetMapping
    public DiagnosisHistoryResponseDTO recent(@LoginMember Long memberId) { return historyService.recent(memberId); }

    @PutMapping("/{id}")
    public DiagnosisResponseDTO reevaluate(@LoginMember Long memberId, @PathVariable long id,
                                           @RequestBody DiagnosisRequestDTO request) {
        return diagnosisService.reevaluate(memberId, id, request);
    }
}
