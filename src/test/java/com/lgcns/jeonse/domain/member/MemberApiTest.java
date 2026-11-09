package com.lgcns.jeonse.domain.member;

import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.lgcns.jeonse.global.security.JwtProvider;
import com.lgcns.jeonse.global.security.LoginMember;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootTest
@AutoConfigureMockMvc
@Import(MemberApiTest.OptionalAuthProbe.class)
class MemberApiTest {
    @Autowired MockMvc mvc;

    /** 2번 진단처럼 토큰이 선택인 API를 흉내 낸다. */
    @RestController
    static class OptionalAuthProbe {
        @GetMapping("/test/optional-auth")
        String probe(@LoginMember(required = false) Long memberId) { return String.valueOf(memberId); }
    }

    private static String uniqueEmail() { return "user-" + UUID.randomUUID() + "@example.com"; }

    private ResultActions signup(String name, String email, String password) throws Exception {
        return mvc.perform(post("/api/members").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"" + name + "\",\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"));
    }

    private ResultActions login(String email, String password) throws Exception {
        return mvc.perform(post("/api/members/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"));
    }

    private String tokenOf(ResultActions result) throws Exception {
        return JsonPath.read(result.andReturn().getResponse().getContentAsString(), "$.accessToken");
    }

    @Test
    void 가입하면_소문자_이메일로_저장하고_토큰으로_내_정보를_본다() throws Exception {
        String email = uniqueEmail();
        String token = tokenOf(signup("  김전세 ", "  " + email.toUpperCase() + " ", "password123")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.member.name").value("김전세"))
                .andExpect(jsonPath("$.member.email").value(email)));

        mvc.perform(get("/api/members/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("김전세"))
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    void 대소문자만_다른_이메일로_다시_가입하면_409() throws Exception {
        String email = uniqueEmail();
        signup("김전세", email, "password123").andExpect(status().isCreated());
        signup("김전세", email.toUpperCase(), "password123")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("DUPLICATE_EMAIL"))
                .andExpect(jsonPath("$.error.field").value("email"));
    }

    @Test
    void 가입_입력이_규칙에_안_맞으면_400과_field() throws Exception {
        signup(" ", uniqueEmail(), "password123")
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error.field").value("name"));
        signup("김전세", "not-an-email", "password123")
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error.field").value("email"));
        signup("김전세", uniqueEmail(), "short")
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error.field").value("password"));
        signup("김전세", uniqueEmail(), "a".repeat(73))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error.field").value("password"));
        mvc.perform(post("/api/members").contentType(MediaType.APPLICATION_JSON).content("{broken"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_INPUT"))
                .andExpect(jsonPath("$.error.field").value(nullValue()));
    }

    @Test
    void 로그인은_맞으면_토큰_틀리면_401() throws Exception {
        String email = uniqueEmail();
        signup("김전세", email, "password123").andExpect(status().isCreated());

        String token = tokenOf(login(email.toUpperCase(), "password123").andExpect(status().isOk())
                .andExpect(jsonPath("$.member.email").value(email)));
        mvc.perform(get("/api/members/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk());

        login(email, "wrong-password")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.error.field").value("password"));
        login(uniqueEmail(), "password123").andExpect(status().isUnauthorized());
        login(email, "a".repeat(100)).andExpect(status().isUnauthorized());
    }

    @Test
    void 내_정보는_토큰이_없거나_잘못되면_401() throws Exception {
        mvc.perform(get("/api/members/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
        mvc.perform(get("/api/members/me").header(HttpHeaders.AUTHORIZATION, "Bearer not.a.jwt"))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/members/me").header(HttpHeaders.AUTHORIZATION, "Basic abc"))
                .andExpect(status().isUnauthorized());
        String otherKeyToken = new JwtProvider("another-secret-key-at-least-32-bytes!!").issue(1);
        mvc.perform(get("/api/members/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + otherKeyToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void 긴_JWT_SECRET이어도_HS256으로_서명한다() {
        String token = new JwtProvider("x".repeat(64)).issue(1);
        String header = new String(java.util.Base64.getUrlDecoder().decode(token.split("\\.")[0]));
        org.assertj.core.api.Assertions.assertThat(header).contains("\"alg\":\"HS256\"");
    }

    @Test
    void 토큰_선택_API는_토큰이_없으면_비회원으로_통과한다() throws Exception {
        mvc.perform(get("/test/optional-auth")).andExpect(status().isOk()).andExpect(content().string("null"));

        String token = tokenOf(signup("김전세", uniqueEmail(), "password123"));
        String memberId = JsonPath.read(mvc.perform(get("/api/members/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)).andReturn().getResponse().getContentAsString(), "$.id").toString();
        mvc.perform(get("/test/optional-auth").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(content().string(memberId));

        mvc.perform(get("/test/optional-auth").header(HttpHeaders.AUTHORIZATION, "Bearer broken"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void 질문_API는_잘못된_토큰이_와도_막히지_않는다() throws Exception {
        mvc.perform(get("/api/questions").header(HttpHeaders.AUTHORIZATION, "Bearer broken"))
                .andExpect(status().isOk());
    }
}
