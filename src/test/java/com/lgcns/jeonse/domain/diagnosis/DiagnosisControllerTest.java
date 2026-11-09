package com.lgcns.jeonse.domain.diagnosis;

import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest(properties = "anthropic.api-key=")
@AutoConfigureMockMvc
class DiagnosisControllerTest {
    private static final String ANSWERS = """
            "answers": [
              {"code": "T1_ADDRESS", "answer": "YES"}, {"code": "T2_USAGE", "answer": "NO"},
              {"code": "A1_OWNER", "answer": "YES"}, {"code": "A2_RECENT_OWNER", "answer": "YES"},
              {"code": "A3_TRUST", "answer": "NO"}, {"code": "A4_SEIZURE", "answer": "NO"},
              {"code": "B1_MORTGAGE", "answer": "YES"}, {"code": "B2_LEASE_REG", "answer": "NO"},
              {"code": "B3_JEONSE_RIGHT", "answer": "NO"}, {"code": "R1_VIOLATION", "answer": "NO"}
            ]""";

    @Autowired MockMvc mockMvc;

    private ResultActions send(String body) throws Exception {
        return mockMvc.perform(post("/api/diagnoses").contentType(MediaType.APPLICATION_JSON).content(body));
    }

    @Test
    void caseB_returns201WithContractFields() throws Exception {
        send("{\"deposit\": 24000, \"salePrice\": 28000, \"mortgageMax\": 3600, \"seniorDeposit\": 0, " + ANSWERS + "}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(nullValue()))
                .andExpect(jsonPath("$.score").value(70))
                .andExpect(jsonPath("$.grade").value("DANGER"))
                .andExpect(jsonPath("$.gradeLabel").value("위험"))
                .andExpect(jsonPath("$.gradeMessage").value("계약을 다시 생각해 보세요. 전문가 상담을 권해요."))
                .andExpect(jsonPath("$.debtRatio").value(98.6))
                .andExpect(jsonPath("$.factors[0].code").value("DEBT_RATIO"))
                .andExpect(jsonPath("$.factors[3].code").value("B1_MORTGAGE"))
                .andExpect(jsonPath("$.factors[3].score").value(0))
                .andExpect(jsonPath("$.commonAdvice.specialClause").isString())
                .andExpect(jsonPath("$.aiExplanation").isString())
                .andExpect(jsonPath("$.aiUsed").value(false))
                .andExpect(jsonPath("$.warnings").isEmpty())
                .andExpect(jsonPath("$.disclaimer").isString())
                .andExpect(jsonPath("$.createdAt").value(matchesPattern("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}")));
    }

    @Test
    void invalidAmount_returns400WithField() throws Exception {
        send("{\"deposit\": 0, \"salePrice\": 28000, " + ANSWERS + "}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_INPUT"))
                .andExpect(jsonPath("$.error.message").value("보증금은 0보다 커야 해요"))
                .andExpect(jsonPath("$.error.field").value("deposit"));
    }

    @Test
    void decimalAmount_returns400WithField() throws Exception {
        send("{\"deposit\": 24000.5, \"salePrice\": 28000, " + ANSWERS + "}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_INPUT"))
                .andExpect(jsonPath("$.error.field").value("deposit"));
    }

    @Test
    void malformedJson_returns400WithoutField() throws Exception {
        send("{\"deposit\": ")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_INPUT"))
                .andExpect(jsonPath("$.error.field").value(nullValue()));
    }
}
