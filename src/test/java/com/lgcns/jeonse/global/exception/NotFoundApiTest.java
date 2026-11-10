package com.lgcns.jeonse.global.exception;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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

// docs/03 2장 공통 에러: 없는 경로는 404 NOT_FOUND, field null
@SpringBootTest
@AutoConfigureMockMvc
class NotFoundApiTest {
    @Autowired MockMvc mvc;

    @Test
    void 없는_API_경로는_404_NOT_FOUND() throws Exception {
        expectNotFound(mvc.perform(get("/api/없는경로")));
        expectNotFound(mvc.perform(get("/api/questions/1")));
        expectNotFound(mvc.perform(post("/api/nothing").contentType(MediaType.APPLICATION_JSON).content("{}")));
    }

    @Test
    void API_밖의_없는_경로도_같은_형식의_404() throws Exception {
        expectNotFound(mvc.perform(get("/no-such-page")));
    }

    private static void expectNotFound(ResultActions result) throws Exception {
        result.andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.error.message").value("요청한 주소를 찾을 수 없어요. 주소를 다시 확인해 주세요."))
                .andExpect(jsonPath("$.error.field").isEmpty());
    }
}
