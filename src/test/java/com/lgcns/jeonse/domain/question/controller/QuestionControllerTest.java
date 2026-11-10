package com.lgcns.jeonse.domain.question.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

// H2에서는 QuestionDataInitializer, CI(MySQL)에서는 db/seed.sql의 10개로 확인한다
@SpringBootTest
@AutoConfigureMockMvc
class QuestionControllerTest {
    @Autowired MockMvc mvc;

    @Test
    void 질문_10개를_순서대로_돌려주고_점수_정보는_넣지_않는다() throws Exception {
        mvc.perform(get("/api/questions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.questions", hasSize(10)))
                .andExpect(jsonPath("$.questions[0].code").value("T1_ADDRESS"))
                .andExpect(jsonPath("$.questions[6].code").value("B1_MORTGAGE"))
                .andExpect(jsonPath("$.questions[6].text").value("을구에 \"근저당권\"이 (빨간 줄 없이) 있나요?"))
                .andExpect(jsonPath("$.questions[6].needsAmount").value(true))
                .andExpect(jsonPath("$.questions[9].order").value(10))
                .andExpect(jsonPath("$.questions[0].riskyAnswer").doesNotExist())
                .andExpect(jsonPath("$.questions[0].riskScore").doesNotExist());
    }
}
