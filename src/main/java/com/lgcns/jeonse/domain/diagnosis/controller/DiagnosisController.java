package com.lgcns.jeonse.domain.diagnosis.controller;

import com.lgcns.jeonse.domain.diagnosis.dto.DiagnosisRequestDTO;
import com.lgcns.jeonse.domain.diagnosis.dto.DiagnosisResponseDTO;
import com.lgcns.jeonse.domain.diagnosis.service.DiagnosisService;
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

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DiagnosisResponseDTO diagnose(@RequestBody DiagnosisRequestDTO request) { return service.diagnose(request); }
}
