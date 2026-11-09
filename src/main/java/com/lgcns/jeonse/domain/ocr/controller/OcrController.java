package com.lgcns.jeonse.domain.ocr.controller;

import com.lgcns.jeonse.domain.ocr.dto.OcrResponseDTO;
import com.lgcns.jeonse.domain.ocr.service.OcrService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/ocr")
public class OcrController {
    private final OcrService service;
    public OcrController(OcrService service) { this.service = service; }

    // 필수 값 검사는 서비스에서 docs/03 에러 형식(field 포함)으로 한다
    @PostMapping("/registry")
    public OcrResponseDTO readRegistry(@RequestParam(required = false) String address,
                                       @RequestParam(required = false) MultipartFile registry,
                                       @RequestParam(required = false) MultipartFile buildingLedger) {
        return service.read(address, registry, buildingLedger);
    }
}
