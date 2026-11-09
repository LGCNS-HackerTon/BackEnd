package com.lgcns.jeonse.domain.question.controller;

import com.lgcns.jeonse.domain.question.dto.QuestionResponseDTO;
import com.lgcns.jeonse.domain.question.service.QuestionService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/questions")
public class QuestionController {
    private final QuestionService service;
    public QuestionController(QuestionService service) { this.service = service; }
    @GetMapping public QuestionResponseDTO getQuestions() { return service.getQuestions(); }
}
