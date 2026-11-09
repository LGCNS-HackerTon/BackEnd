package com.lgcns.jeonse.domain.schedule.controller;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

// H2에서는 ChecklistDataInitializer, CI(MySQL)에서는 db/seed.sql의 16개로 확인한다
@SpringBootTest
@AutoConfigureMockMvc
class ScheduleControllerTest {
    @Autowired MockMvc mvc;

    @Test
    void 할_일_16개를_돌려준다() throws Exception {
        mvc.perform(get("/api/schedule?contractDate=2026-11-20&balanceDate=2026-12-20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.expiryDate").value("2028-12-20"))
                .andExpect(jsonPath("$.items", hasSize(16)))
                .andExpect(jsonPath("$.items[0].order").value(1))
                .andExpect(jsonPath("$.items[0].date").value("2026-11-19"))
                .andExpect(jsonPath("$.items[0].link.name").value("인터넷등기소"))
                .andExpect(jsonPath("$.items[?(@.important == true)].order", containsInAnyOrder(9, 11)));
    }

    @Test
    void 잘못된_입력은_INVALID_INPUT과_필드를_돌려준다() throws Exception {
        mvc.perform(get("/api/schedule?balanceDate=2026-12-20"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_INPUT"))
                .andExpect(jsonPath("$.error.field").value("contractDate"));
        mvc.perform(get("/api/schedule?contractDate=2026-13-01&balanceDate=2026-12-20"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.field").value("contractDate"));
        mvc.perform(get("/api/schedule?contractDate=2026-11-20&balanceDate=2026-12-20&termMonths=abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.field").value("termMonths"));
    }
}
