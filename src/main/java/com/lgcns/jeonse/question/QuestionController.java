package com.lgcns.jeonse.question;

import com.lgcns.jeonse.question.dto.QuestionResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/questions")
public class QuestionController {
    private final QuestionService service;
    public QuestionController(QuestionService service) { this.service = service; }
    @GetMapping public QuestionResponse getQuestions() { return service.getQuestions(); }
}
