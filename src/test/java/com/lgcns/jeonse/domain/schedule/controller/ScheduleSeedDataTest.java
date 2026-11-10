package com.lgcns.jeonse.domain.schedule.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.lgcns.jeonse.domain.schedule.init.ChecklistDataInitializer;
import com.lgcns.jeonse.domain.schedule.repository.ChecklistItemRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

// 할 일이 일부만 있으면 불완전한 일정표를 200으로 주지 않고, 초기화를 다시 돌리면 빠진 행만 복구된다
@SpringBootTest
@AutoConfigureMockMvc
class ScheduleSeedDataTest {
    private static final String URL = "/api/schedule?contractDate=2026-11-20&balanceDate=2026-12-20";
    @Autowired MockMvc mvc;
    @Autowired ChecklistItemRepository repository;

    @AfterEach
    void 다른_테스트를_위해_복구한다() {
        ChecklistDataInitializer.fillMissing(repository);
    }

    @Test
    void 할_일이_빠지면_500_INTERNAL_ERROR_다시_채우면_16개() throws Exception {
        repository.deleteAll(repository.findAll().stream()
                .filter(item -> item.getSortOrder() == 9 || item.getSortOrder() == 11).toList());

        mvc.perform(get(URL))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error.code").value("INTERNAL_ERROR"));

        ChecklistDataInitializer.fillMissing(repository);

        mvc.perform(get(URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(16)))
                .andExpect(jsonPath("$.items[?(@.important == true)].order").value(org.hamcrest.Matchers.containsInAnyOrder(9, 11)));
    }
}
