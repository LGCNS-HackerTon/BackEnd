package com.lgcns.jeonse.domain.schedule.init;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.lgcns.jeonse.domain.schedule.entity.ChecklistItem;
import com.lgcns.jeonse.domain.schedule.repository.ChecklistItemRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class ChecklistDataInitializerTest {
    private final ChecklistItemRepository repository = mock(ChecklistItemRepository.class);

    @Test
    void 기본_할_일은_순서_1부터_16까지_하나씩이다() {
        assertThat(ChecklistDataInitializer.defaults()).extracting(ChecklistItem::getSortOrder)
                .containsExactlyInAnyOrder(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16);
    }

    @Test
    @SuppressWarnings("unchecked")
    void 일부만_있으면_빠진_순서만_넣고_있는_행은_건드리지_않는다() {
        List<ChecklistItem> existing = ChecklistDataInitializer.defaults().stream()
                .filter(item -> item.getSortOrder() != 9 && item.getSortOrder() != 11 && item.getSortOrder() != 15).toList();
        when(repository.findAll()).thenReturn(existing);

        assertThat(ChecklistDataInitializer.fillMissing(repository)).isEqualTo(3);
        ArgumentCaptor<List<ChecklistItem>> saved = ArgumentCaptor.forClass(List.class);
        verify(repository).saveAll(saved.capture());
        assertThat(saved.getValue()).extracting(ChecklistItem::getSortOrder).containsExactly(9, 11, 15);
        verify(repository, never()).deleteAll();
        verify(repository, never()).deleteAll(any());
        verify(repository, never()).save(any());
    }

    @Test
    void 모두_있으면_아무것도_넣지_않는다() {
        when(repository.findAll()).thenReturn(ChecklistDataInitializer.defaults());

        assertThat(ChecklistDataInitializer.fillMissing(repository)).isZero();
        verify(repository, never()).saveAll(any());
    }
}
