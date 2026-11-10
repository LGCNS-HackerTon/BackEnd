package com.lgcns.jeonse.global.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.lgcns.jeonse.domain.diagnosis.repository.DiagnosisAnswerRepository;
import com.lgcns.jeonse.domain.diagnosis.repository.DiagnosisRepository;
import com.lgcns.jeonse.domain.member.repository.MemberRepository;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

// docs/03 6.1·8번: 서명·만료가 정상이어도 회원이 없는 토큰은 401. 토큰을 읽지 않는 1·4번은 영향 없음
@SpringBootTest(properties = "anthropic.api-key=")
@AutoConfigureMockMvc
class DeletedMemberTokenTest {
    private static final String BODY = """
            {"deposit":24000,"salePrice":28000,"mortgageMax":0,"seniorDeposit":0,"answers":[
             {"code":"T1_ADDRESS","answer":"YES"},{"code":"T2_USAGE","answer":"YES"},
             {"code":"A1_OWNER","answer":"YES"},{"code":"A2_RECENT_OWNER","answer":"NO"},
             {"code":"A3_TRUST","answer":"NO"},{"code":"A4_SEIZURE","answer":"NO"},
             {"code":"B1_MORTGAGE","answer":"NO"},{"code":"B2_LEASE_REG","answer":"NO"},
             {"code":"B3_JEONSE_RIGHT","answer":"NO"},{"code":"R1_VIOLATION","answer":"NO"}]}""";

    @Autowired MockMvc mvc;
    @Autowired MemberRepository members;
    @Autowired DiagnosisRepository diagnoses;
    @Autowired DiagnosisAnswerRepository answers;

    private String deletedMemberToken;

    // 가입해서 받은 정상 토큰을 두고 회원만 지운다 (탈퇴·DB 초기화와 같은 상황)
    @BeforeEach
    void 가입한_뒤_회원을_지운다() throws Exception {
        String response = signup();
        deletedMemberToken = JsonPath.read(response, "$.accessToken");
        members.deleteById(((Number) JsonPath.read(response, "$.member.id")).longValue());
    }

    private String signup() throws Exception {
        return mvc.perform(post("/api/auth/signup").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"김전세\",\"email\":\"" + UUID.randomUUID() + "@example.com\",\"password\":\"password123\"}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
    }

    private MockHttpServletRequestBuilder withToken(MockHttpServletRequestBuilder request) {
        return request.header(HttpHeaders.AUTHORIZATION, "Bearer " + deletedMemberToken);
    }

    private static void expectUnauthorized(ResultActions result) throws Exception {
        result.andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.error.field").value(nullValue()));
    }

    @Test
    void 회원이_없는_토큰은_2_8_9_10번_모두_401() throws Exception {
        String ownerToken = JsonPath.read(signup(), "$.accessToken");
        String savedDiagnosis = mvc.perform(post("/api/diagnoses")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long diagnosisId = ((Number) JsonPath.read(savedDiagnosis, "$.id")).longValue();
        long diagnosisCount = diagnoses.count();
        long answerCount = answers.count();
        expectUnauthorized(mvc.perform(withToken(get("/api/members/me"))));
        expectUnauthorized(mvc.perform(withToken(post("/api/diagnoses"))
                .contentType(MediaType.APPLICATION_JSON).content(BODY)));
        expectUnauthorized(mvc.perform(withToken(get("/api/members/me/diagnoses"))));
        expectUnauthorized(mvc.perform(withToken(put("/api/members/me/diagnoses/" + diagnosisId))
                .contentType(MediaType.APPLICATION_JSON).content(BODY)));
        assertThat(diagnoses.count()).isEqualTo(diagnosisCount);
        assertThat(answers.count()).isEqualTo(answerCount);
    }

    @Test
    void 토큰_없는_비회원_진단은_그대로_201() throws Exception {
        mvc.perform(post("/api/diagnoses").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(nullValue()));
    }

    @Test
    void 질문과_일정표는_토큰을_읽지_않는다() throws Exception {
        mvc.perform(withToken(get("/api/questions"))).andExpect(status().isOk());
        mvc.perform(withToken(get("/api/schedule?contractDate=2026-11-20&balanceDate=2026-12-20")))
                .andExpect(status().isOk());
    }
}
