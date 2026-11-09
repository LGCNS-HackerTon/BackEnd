package com.lgcns.jeonse.domain.schedule.controller;

import com.lgcns.jeonse.domain.schedule.dto.ScheduleResponseDTO;
import com.lgcns.jeonse.domain.schedule.service.ScheduleService;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/schedule")
public class ScheduleController {
    private final ScheduleService service;
    public ScheduleController(ScheduleService service) { this.service = service; }

    // 필수 날짜 검사는 쉬운 안내 문구("계약일을 입력해 주세요.")를 위해 서비스에서 한다
    @GetMapping
    public ScheduleResponseDTO getSchedule(
            @RequestParam(required = false) @DateTimeFormat(iso = ISO.DATE) LocalDate contractDate,
            @RequestParam(required = false) @DateTimeFormat(iso = ISO.DATE) LocalDate balanceDate,
            @RequestParam(required = false) @DateTimeFormat(iso = ISO.DATE) LocalDate moveInDate,
            @RequestParam(required = false) Integer termMonths) {
        return service.getSchedule(contractDate, balanceDate, moveInDate, termMonths);
    }
}
