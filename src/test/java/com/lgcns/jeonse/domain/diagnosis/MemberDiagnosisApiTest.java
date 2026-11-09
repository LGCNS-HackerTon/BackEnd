package com.lgcns.jeonse.domain.diagnosis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.lgcns.jeonse.domain.diagnosis.repository.DiagnosisAnswerRepository;
import com.lgcns.jeonse.domain.diagnosis.repository.DiagnosisRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

// docs/03 6장 2·9·10번 + 데모 계정
@SpringBootTest(properties = "anthropic.api-key=")
@AutoConfigureMockMvc
class MemberDiagnosisApiTest {
    private static final String CASE_B_ANSWERS = """
            [{"code":"T1_ADDRESS","answer":"YES"},{"code":"T2_USAGE","answer":"NO"},
             {"code":"A1_OWNER","answer":"YES"},{"code":"A2_RECENT_OWNER","answer":"YES"},
             {"code":"A3_TRUST","answer":"NO"},{"code":"A4_SEIZURE","answer":"NO"},
             {"code":"B1_MORTGAGE","answer":"YES"},{"code":"B2_LEASE_REG","answer":"NO"},
             {"code":"B3_JEONSE_RIGHT","answer":"NO"},{"code":"R1_VIOLATION","answer":"NO"}]""";
    private static final String SAFE_ANSWERS = CASE_B_ANSWERS
            .replace("\"T2_USAGE\",\"answer\":\"NO\"", "\"T2_USAGE\",\"answer\":\"YES\"")
            .replace("\"A2_RECENT_OWNER\",\"answer\":\"YES\"", "\"A2_RECENT_OWNER\",\"answer\":\"NO\"")
            .replace("\"B1_MORTGAGE\",\"answer\":\"YES\"", "\"B1_MORTGAGE\",\"answer\":\"NO\"");

    @Autowired MockMvc mvc;
    @Autowired DiagnosisRepository diagnoses;
    @Autowired DiagnosisAnswerRepository answers;

    private static String body(int deposit, String seniorDeposit, String answers) {
        return "{\"deposit\":" + deposit + ",\"salePrice\":28000,\"mortgageMax\":3600,\"seniorDeposit\":"
                + seniorDeposit + ",\"answers\":" + answers + "}";
    }

    private static MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder request, String token, String body) {
        if (token != null) request.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        return request.contentType(MediaType.APPLICATION_JSON).content(body);
    }

    private ResultActions diagnose(String token, String body) throws Exception {
        return mvc.perform(json(post("/api/diagnoses"), token, body));
    }

    private ResultActions history(String token) throws Exception {
        return mvc.perform(get("/api/members/me/diagnoses").header(HttpHeaders.AUTHORIZATION, "Bearer " + token));
    }

    private String newMemberToken() throws Exception {
        String response = mvc.perform(post("/api/auth/signup").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"김전세\",\"email\":\"" + UUID.randomUUID() + "@example.com\",\"password\":\"password123\"}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return JsonPath.read(response, "$.accessToken");
    }

    private static long idOf(ResultActions result) throws Exception {
        return ((Number) JsonPath.read(result.andReturn().getResponse().getContentAsString(), "$.id")).longValue();
    }

    @Test
    void 회원_진단은_저장하고_비회원_진단은_저장하지_않는다() throws Exception {
        long diagnosisCount = diagnoses.count(), answerCount = answers.count();
        diagnose(null, body(24000, "0", CASE_B_ANSWERS))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.id").value(nullValue()));
        assertThat(diagnoses.count()).isEqualTo(diagnosisCount);

        String token = newMemberToken();
        long id = idOf(diagnose(token, body(24000, "0", CASE_B_ANSWERS))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(notNullValue()))
                .andExpect(jsonPath("$.score").value(70))
                .andExpect(jsonPath("$.grade").value("DANGER")));
        assertThat(diagnoses.count()).isEqualTo(diagnosisCount + 1);
        assertThat(answers.count()).isEqualTo(answerCount + 10);

        history(token).andExpect(status().isOk())
                .andExpect(jsonPath("$.diagnoses.length()").value(1))
                .andExpect(jsonPath("$.diagnoses[0].id").value(id))
                .andExpect(jsonPath("$.diagnoses[0].location").value("보증금 2억 4,000만 원 · 시세 2억 8,000만 원"))
                .andExpect(jsonPath("$.diagnoses[0].updatedAt").value(nullValue()))
                .andExpect(jsonPath("$.diagnoses[0].score").value(70))
                .andExpect(jsonPath("$.diagnoses[0].grade").value("DANGER"))
                .andExpect(jsonPath("$.diagnoses[0].gradeLabel").value("위험"))
                .andExpect(jsonPath("$.diagnoses[0].request.deposit").value(24000))
                .andExpect(jsonPath("$.diagnoses[0].request.salePrice").value(28000))
                .andExpect(jsonPath("$.diagnoses[0].request.mortgageMax").value(3600))
                .andExpect(jsonPath("$.diagnoses[0].request.seniorDeposit").value(0))
                .andExpect(jsonPath("$.diagnoses[0].request.answers.length()").value(10))
                .andExpect(jsonPath("$.diagnoses[0].request.answers[0].code").value("T1_ADDRESS"))
                .andExpect(jsonPath("$.diagnoses[0].request.answers[9].code").value("R1_VIOLATION"));
    }

    @Test
    void 여섯_건을_저장하면_최근_5건을_오래된_순서로_돌려준다() throws Exception {
        String token = newMemberToken();
        for (int deposit = 10001; deposit <= 10006; deposit++)
            diagnose(token, body(deposit, "null", SAFE_ANSWERS)).andExpect(status().isCreated());

        String response = history(token).andExpect(status().isOk())
                .andExpect(jsonPath("$.diagnoses.length()").value(5))
                .andExpect(jsonPath("$.diagnoses[0].request.seniorDeposit").value(nullValue()))
                .andExpect(jsonPath("$.diagnoses[0].request.mortgageMax").value(0)) // 근저당 NO면 0
                .andReturn().getResponse().getContentAsString();
        List<Integer> deposits = JsonPath.read(response, "$.diagnoses[*].request.deposit");
        assertThat(deposits).containsExactly(10002, 10003, 10004, 10005, 10006);
        List<String> createdAts = JsonPath.read(response, "$.diagnoses[*].createdAt");
        assertThat(createdAts).isSorted();
    }

    @Test
    void 재평가하면_같은_행을_덮어쓴다() throws Exception {
        String token = newMemberToken();
        long id = idOf(diagnose(token, body(24000, "0", CASE_B_ANSWERS)));
        String createdAt = JsonPath.read(history(token).andReturn().getResponse().getContentAsString(),
                "$.diagnoses[0].createdAt");
        long diagnosisCount = diagnoses.count(), answerCount = answers.count();
        Thread.sleep(1100); // 시각은 초 단위라 재평가 시각이 달라지게 기다린다

        mvc.perform(json(put("/api/members/me/diagnoses/" + id), token, body(10000, "null", SAFE_ANSWERS)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.score").value(0))
                .andExpect(jsonPath("$.grade").value("SAFE"))
                .andExpect(jsonPath("$.createdAt").value(createdAt))
                .andExpect(jsonPath("$.warnings.length()").value(1))
                .andExpect(jsonPath("$.disclaimer").value(notNullValue()));

        assertThat(diagnoses.count()).isEqualTo(diagnosisCount);
        assertThat(answers.count()).isEqualTo(answerCount);
        history(token)
                .andExpect(jsonPath("$.diagnoses.length()").value(1))
                .andExpect(jsonPath("$.diagnoses[0].createdAt").value(createdAt))
                .andExpect(jsonPath("$.diagnoses[0].updatedAt").value(notNullValue()))
                .andExpect(jsonPath("$.diagnoses[0].location").value("보증금 1억 원 · 시세 2억 8,000만 원"))
                .andExpect(jsonPath("$.diagnoses[0].score").value(0))
                .andExpect(jsonPath("$.diagnoses[0].gradeLabel").value("안전"))
                .andExpect(jsonPath("$.diagnoses[0].request.mortgageMax").value(0))
                .andExpect(jsonPath("$.diagnoses[0].request.seniorDeposit").value(nullValue()))
                .andExpect(jsonPath("$.diagnoses[0].request.answers[1].answer").value("YES"))
                .andExpect(jsonPath("$.diagnoses[0].request.answers[6].answer").value("NO"));
    }

    @Test
    void 남의_기록이나_없는_id를_재평가하면_404() throws Exception {
        long id = idOf(diagnose(newMemberToken(), body(24000, "0", CASE_B_ANSWERS)));
        String other = newMemberToken();
        mvc.perform(json(put("/api/members/me/diagnoses/" + id), other, body(10000, "0", SAFE_ANSWERS)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.error.message").value("진단 기록을 찾을 수 없어요."))
                .andExpect(jsonPath("$.error.field").value(nullValue()));
        mvc.perform(json(put("/api/members/me/diagnoses/999999999"), other, body(10000, "0", SAFE_ANSWERS)))
                .andExpect(status().isNotFound());
        history(other).andExpect(jsonPath("$.diagnoses.length()").value(0));
    }

    @Test
    void 기록_API는_토큰이_없으면_401_진단은_잘못된_토큰이면_401() throws Exception {
        mvc.perform(get("/api/members/me/diagnoses"))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
        mvc.perform(json(put("/api/members/me/diagnoses/1"), null, body(10000, "0", SAFE_ANSWERS)))
                .andExpect(status().isUnauthorized());
        long diagnosisCount = diagnoses.count();
        diagnose("broken", body(24000, "0", CASE_B_ANSWERS))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
        assertThat(diagnoses.count()).isEqualTo(diagnosisCount);
    }

    @Test
    void 데모_계정으로_로그인하고_기록은_비어_있다() throws Exception {
        String response = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"demo@example.com\",\"password\":\"jeonse-demo-2026\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.member.name").value("데모 사용자"))
                .andExpect(jsonPath("$.member.email").value("demo@example.com"))
                .andReturn().getResponse().getContentAsString();
        String token = JsonPath.read(response, "$.accessToken");
        mvc.perform(get("/api/members/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(jsonPath("$.name").value("데모 사용자"));
    }
}
