package com.lgcns.jeonse.domain.schedule.init;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.lgcns.jeonse.domain.schedule.entity.ChecklistItem;
import com.lgcns.jeonse.domain.schedule.repository.ChecklistItemRepository;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class ChecklistDataInitializerTest {
    private final ChecklistItemRepository repository = mock(ChecklistItemRepository.class);

    // 저장된 행처럼 id = 순서 번호를 붙인다 (db/seed.sql과 같음)
    private static List<ChecklistItem> stored(List<ChecklistItem> items) {
        items.forEach(item -> ReflectionTestUtils.setField(item, "id", (long) item.getSortOrder()));
        return items;
    }

    private void verifyNothingElseWritten() {
        verify(repository, never()).save(any());
        verify(repository, never()).saveAll(any());
        verify(repository, never()).deleteAll();
        verify(repository, never()).deleteAll(any());
    }

    @Test
    void 기본_할_일은_순서_1부터_16까지_하나씩이다() {
        assertThat(ChecklistDataInitializer.defaults()).extracting(ChecklistItem::getSortOrder)
                .containsExactlyInAnyOrder(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16);
    }

    @Test
    void 빈_테이블이면_16개를_id와_순서를_같게_넣는다() {
        when(repository.findAll()).thenReturn(List.of());

        assertThat(ChecklistDataInitializer.fillMissing(repository)).isEqualTo(16);
        for (int order = 1; order <= 16; order++)
            verify(repository).insertWithId(eq((long) order), eq(order), anyString(), anyInt(), anyString(), anyString(),
                    anyBoolean(), any(), any());
        verifyNothingElseWritten();
    }

    @Test
    void 일부만_있으면_빠진_순서만_seed와_같은_id로_넣고_있는_행은_건드리지_않는다() {
        List<ChecklistItem> existing = stored(ChecklistDataInitializer.defaults().stream()
                .filter(item -> item.getSortOrder() != 9 && item.getSortOrder() != 11 && item.getSortOrder() != 15)
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll));
        when(repository.findAll()).thenReturn(existing);

        assertThat(ChecklistDataInitializer.fillMissing(repository)).isEqualTo(3);
        verify(repository).insertWithId(eq(9L), eq(9), eq("BALANCE"), eq(0), anyString(), anyString(), eq(true),
                eq("인터넷등기소"), eq("https://www.iros.go.kr"));
        verify(repository).insertWithId(eq(11L), eq(11), eq("MOVE_IN"), eq(0), anyString(), anyString(), eq(true),
                eq("정부24"), eq("https://www.gov.kr"));
        verify(repository).insertWithId(eq(15L), eq(15), anyString(), anyInt(), anyString(), anyString(), anyBoolean(),
                any(), any());
        verify(repository, times(3)).insertWithId(anyLong(), anyInt(), anyString(), anyInt(), anyString(), anyString(),
                anyBoolean(), any(), any());
        verifyNothingElseWritten();
    }

    @Test
    void 모두_있으면_아무것도_넣지_않는다() {
        when(repository.findAll()).thenReturn(stored(new ArrayList<>(ChecklistDataInitializer.defaults())));

        assertThat(ChecklistDataInitializer.fillMissing(repository)).isZero();
        verify(repository, never()).insertWithId(anyLong(), anyInt(), anyString(), anyInt(), anyString(), anyString(),
                anyBoolean(), any(), any());
        verifyNothingElseWritten();
    }

    @Test
    void 빠진_순서의_id를_다른_행이_쓰면_넣지_않는다() {
        List<ChecklistItem> existing = stored(ChecklistDataInitializer.defaults().stream()
                .filter(item -> item.getSortOrder() != 7)
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll));
        ReflectionTestUtils.setField(existing.get(0), "id", 7L); // 1번 행이 id 7을 쓰고 있음
        when(repository.findAll()).thenReturn(existing);

        assertThat(ChecklistDataInitializer.fillMissing(repository)).isZero();
        verify(repository, never()).insertWithId(anyLong(), anyInt(), anyString(), anyInt(), anyString(), anyString(),
                anyBoolean(), any(), any());
    }
}
