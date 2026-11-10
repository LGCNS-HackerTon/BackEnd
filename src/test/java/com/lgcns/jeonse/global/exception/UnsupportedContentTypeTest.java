package com.lgcns.jeonse.global.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

// docs/03 2장: JSON 형식 오류는 400 INVALID_INPUT, field null. Content-Type이 없거나 JSON이 아닌 경우도 같다
@SpringBootTest
@AutoConfigureMockMvc
@ExtendWith(OutputCaptureExtension.class)
class UnsupportedContentTypeTest {
    private static final String DIAGNOSIS = """
            {"deposit":24000,"salePrice":28000,"mortgageMax":0,"seniorDeposit":0,"answers":[
             {"code":"T1_ADDRESS","answer":"YES"},{"code":"T2_USAGE","answer":"YES"},
             {"code":"A1_OWNER","answer":"YES"},{"code":"A2_RECENT_OWNER","answer":"NO"},
             {"code":"A3_TRUST","answer":"NO"},{"code":"A4_SEIZURE","answer":"NO"},
             {"code":"B1_MORTGAGE","answer":"NO"},{"code":"B2_LEASE_REG","answer":"NO"},
             {"code":"B3_JEONSE_RIGHT","answer":"NO"},{"code":"R1_VIOLATION","answer":"NO"}]}""";

    @Autowired MockMvc mvc;

    private static String uniqueEmail() { return "ct-" + UUID.randomUUID() + "@example.com"; }

    private String memberToken() throws Exception {
        String response = mvc.perform(post("/api/auth/signup").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"김전세\",\"email\":\"" + uniqueEmail() + "\",\"password\":\"password123\"}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return JsonPath.read(response, "$.accessToken");
    }

    // 같은 JSON 본문을 Content-Type 없이, text/plain으로 보내면 둘 다 400 INVALID_INPUT
    private void expectInvalidInput(MockHttpServletRequestBuilder request, String body) throws Exception {
        for (String contentType : new String[] {null, MediaType.TEXT_PLAIN_VALUE}) {
            MockHttpServletRequestBuilder sent = request.content(body);
            if (contentType != null) sent.contentType(contentType);
            mvc.perform(sent)
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error.code").value("INVALID_INPUT"))
                    .andExpect(jsonPath("$.error.message").value("입력 형식이 올바르지 않아요. 다시 확인해 주세요."))
                    .andExpect(jsonPath("$.error.field").value(nullValue()));
        }
    }

    @Test
    void 회원가입과_로그인() throws Exception {
        expectInvalidInput(post("/api/auth/signup"),
                "{\"name\":\"김전세\",\"email\":\"" + uniqueEmail() + "\",\"password\":\"password123\"}");
        expectInvalidInput(post("/api/auth/login"), "{\"email\":\"demo@example.com\",\"password\":\"jeonse-demo-2026\"}");
    }

    @Test
    void 진단과_재평가() throws Exception {
        expectInvalidInput(post("/api/diagnoses"), DIAGNOSIS);
        // 10번은 토큰을 먼저 확인하므로 정상 토큰을 붙여 본문 단계까지 보낸다
        String token = memberToken();
        expectInvalidInput(put("/api/members/me/diagnoses/1").header(HttpHeaders.AUTHORIZATION, "Bearer " + token),
                DIAGNOSIS);
    }

    @Test
    void 서버_오류_로그를_남기지_않는다(CapturedOutput output) throws Exception {
        expectInvalidInput(post("/api/auth/login"), "{\"email\":\"demo@example.com\",\"password\":\"x\"}");

        assertThat(output).doesNotContain("처리하지 못한 서버 오류");
    }
}
