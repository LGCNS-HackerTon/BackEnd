package com.lgcns.jeonse.global.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

// 처리하지 못한 예외(500)는 응답에서 내부 내용을 숨기고, 서버 로그에는 요청과 예외를 남긴다 (docs/03 2장)
@SpringBootTest
@AutoConfigureMockMvc
@Import(InternalErrorLogTest.BrokenEndpoint.class)
@ExtendWith(OutputCaptureExtension.class)
class InternalErrorLogTest {
    private static final String SECRET = "내부-오류-상세-0f3a";

    @Autowired MockMvc mvc;

    @RestController
    static class BrokenEndpoint {
        @GetMapping("/api/test/broken")
        String broken() { throw new IllegalStateException(SECRET); }
    }

    @Test
    void 처리하지_못한_예외는_응답은_그대로_로그에는_요청과_예외를_남긴다(CapturedOutput output) throws Exception {
        String body = mvc.perform(get("/api/test/broken"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error.code").value("INTERNAL_ERROR"))
                .andExpect(jsonPath("$.error.message").value("서버 오류가 발생했어요. 잠시 후 다시 시도해 주세요."))
                .andExpect(jsonPath("$.error.field").value(nullValue()))
                .andReturn().getResponse().getContentAsString();

        assertThat(body).doesNotContain(SECRET).doesNotContain("IllegalStateException");
        assertThat(output).contains("ERROR")
                .contains("처리하지 못한 서버 오류")
                .contains("GET /api/test/broken")
                .contains("java.lang.IllegalStateException: " + SECRET) // stack trace 첫 줄
                .contains("at com.lgcns.jeonse.global.exception.InternalErrorLogTest$BrokenEndpoint.broken");
    }

    @Test
    void 정상_오류_응답은_서버_오류_로그를_남기지_않는다(CapturedOutput output) throws Exception {
        mvc.perform(get("/api/no-such-path")).andExpect(status().isNotFound());
        mvc.perform(get("/api/schedule")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/members/me")).andExpect(status().isUnauthorized());

        assertThat(output).doesNotContain("처리하지 못한 서버 오류");
    }
}
