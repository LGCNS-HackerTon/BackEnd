package com.lgcns.jeonse.domain.schedule.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.lgcns.jeonse.domain.schedule.entity.ChecklistItem;
import com.lgcns.jeonse.domain.schedule.init.ChecklistDataInitializer;
import com.lgcns.jeonse.domain.schedule.repository.ChecklistItemRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

// 할 일이 일부만 있으면 불완전한 일정표를 200으로 주지 않고, 초기화를 다시 돌리면 빠진 행만 복구된다
@SpringBootTest
@AutoConfigureMockMvc
class ScheduleSeedDataTest {
    private static final String URL = "/api/schedule?contractDate=2026-11-20&balanceDate=2026-12-20";
    @Autowired MockMvc mvc;
    @Autowired ChecklistItemRepository repository;
    @Autowired JdbcTemplate jdbc;

    @AfterEach
    void 다른_테스트를_위해_복구한다() {
        ChecklistDataInitializer.fillMissing(repository);
    }

    private void deleteOrders(int... orders) {
        for (int order : orders) jdbc.update("DELETE FROM checklist_items WHERE sort_order = ?", order);
    }

    @Test
    void 할_일이_빠지면_500_INTERNAL_ERROR_다시_채우면_16개() throws Exception {
        deleteOrders(9, 11);

        mvc.perform(get(URL))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error.code").value("INTERNAL_ERROR"));

        ChecklistDataInitializer.fillMissing(repository);

        mvc.perform(get(URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(16)))
                .andExpect(jsonPath("$.items[?(@.important == true)].order").value(containsInAnyOrder(9, 11)));
    }

    // db/seed.sql을 다시 실행하는 상황: 순서를 잠시 뒤로 비켜 둔 뒤 id 1~16 기준으로 덮어쓴다 (seed.sql 2절과 같은 방식)
    private void rerunSeedChecklist() {
        jdbc.update("UPDATE checklist_items SET sort_order = sort_order + 16 WHERE sort_order <= 16");
        for (ChecklistItem item : ChecklistDataInitializer.defaults()) {
            int updated = jdbc.update("UPDATE checklist_items SET sort_order = ? WHERE id = ?",
                    item.getSortOrder(), (long) item.getSortOrder());
            if (updated == 0) jdbc.update("""
                    INSERT INTO checklist_items (id, sort_order, anchor, offset_days, title, reason, important, link_name, link_url)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)""", (long) item.getSortOrder(), item.getSortOrder(), item.getAnchor(),
                    item.getOffsetDays(), item.getTitle(), item.getReason(), item.isImportant(), item.getLinkName(),
                    item.getLinkUrl());
        }
    }

    @Test
    void 서버가_복구한_뒤_seed를_다시_실행해도_16개_그대로() throws Exception {
        deleteOrders(7);
        ChecklistDataInitializer.fillMissing(repository);
        // 복구한 행도 seed.sql과 같은 id(= 순서 번호)여야 seed가 같은 행을 덮어쓴다
        assertThat(repository.findAll()).allSatisfy(item -> assertThat(item.getId()).isEqualTo((long) item.getSortOrder()));

        rerunSeedChecklist();

        assertThat(repository.count()).isEqualTo(16);
        mvc.perform(get(URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(16)));
    }
}
