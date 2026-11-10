package com.lgcns.jeonse.domain.schedule.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.lgcns.jeonse.domain.schedule.dto.ScheduleResponseDTO;
import com.lgcns.jeonse.domain.schedule.dto.ScheduleResponseDTO.ScheduleItem;
import com.lgcns.jeonse.domain.schedule.entity.ChecklistItem;
import com.lgcns.jeonse.domain.schedule.init.ChecklistDataInitializer;
import com.lgcns.jeonse.domain.schedule.repository.ChecklistItemRepository;
import com.lgcns.jeonse.global.exception.InvalidInputException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

class ScheduleServiceTest {
    private static final LocalDate TODAY = LocalDate.of(2026, 11, 9);
    private final ChecklistItemRepository repository = mock(ChecklistItemRepository.class);
    private final ScheduleService service = new ScheduleService(repository);

    ScheduleServiceTest() {
        // 실제 기본 데이터 16개를 저장 순서를 뒤집어 넣어 정렬을 확인한다
        List<ChecklistItem> rows = new ArrayList<>(ChecklistDataInitializer.defaults());
        Collections.reverse(rows);
        when(repository.findAll()).thenReturn(rows);
    }

    private ScheduleResponseDTO schedule(String contract, String balance, String moveIn, Integer term, LocalDate today) {
        return service.getSchedule(LocalDate.parse(contract), LocalDate.parse(balance),
                moveIn == null ? null : LocalDate.parse(moveIn), term, today);
    }

    private static ScheduleItem item(ScheduleResponseDTO dto, int order) {
        return dto.items().stream().filter(i -> i.order() == order).findFirst().orElseThrow();
    }

    @Test
    void docs03_예시와_같은_날짜와_남은_날을_계산한다() {
        ScheduleResponseDTO dto = schedule("2026-11-20", "2026-12-20", "2026-12-20", 24, TODAY);

        assertThat(dto.today()).isEqualTo(TODAY);
        assertThat(dto.expiryDate()).isEqualTo(LocalDate.of(2028, 12, 20));
        assertThat(item(dto, 1).date()).isEqualTo(LocalDate.of(2026, 11, 19));
        assertThat(item(dto, 1).daysLeft()).isEqualTo(10);
        assertThat(item(dto, 11).date()).isEqualTo(LocalDate.of(2026, 12, 20));
        assertThat(item(dto, 11).daysLeft()).isEqualTo(41);
        assertThat(item(dto, 14).date()).isEqualTo(LocalDate.of(2028, 10, 21));
        assertThat(item(dto, 1).link().url()).isEqualTo("https://www.iros.go.kr");
        assertThat(item(dto, 14).link()).isNull();
    }

    @Test
    void 날짜_오름차순_같은_날이면_순서_번호순으로_정렬한다() {
        ScheduleResponseDTO dto = schedule("2026-11-20", "2026-12-20", null, null, TODAY);

        // 11-19: 1,2,3,15,16 / 11-20: 4,5,6 / 11-21: 7 / 12-06: 8 / 12-20: 9,10,11 / 12-21: 12 / 만기 전: 13, 14
        assertThat(dto.items()).extracting(ScheduleItem::order)
                .containsExactly(1, 2, 3, 15, 16, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14);
    }

    @Test
    void 입주일이_없으면_잔금일_계약기간이_없으면_24개월로_계산한다() {
        ScheduleResponseDTO dto = schedule("2026-11-20", "2026-12-20", null, null, TODAY);

        assertThat(item(dto, 11).date()).isEqualTo(LocalDate.of(2026, 12, 20));
        assertThat(dto.expiryDate()).isEqualTo(LocalDate.of(2028, 12, 20));
    }

    @Test
    void 만기일이_없는_날이면_그_달_마지막_날로_맞춘다() {
        ScheduleResponseDTO dto = schedule("2026-08-01", "2026-08-31", "2026-08-31", 6, TODAY);

        assertThat(dto.expiryDate()).isEqualTo(LocalDate.of(2027, 2, 28));
    }

    @Test
    void 오늘_기준으로_지난_일_오늘_다가오는_일을_나눈다() {
        ScheduleResponseDTO dto = schedule("2026-11-01", "2026-11-09", "2026-11-10", 24, TODAY);

        assertThat(item(dto, 1).status()).isEqualTo("PAST");
        assertThat(item(dto, 1).daysLeft()).isEqualTo(-9);
        assertThat(item(dto, 9).status()).isEqualTo("TODAY");
        assertThat(item(dto, 9).daysLeft()).isZero();
        assertThat(item(dto, 11).status()).isEqualTo("UPCOMING");
        assertThat(item(dto, 11).daysLeft()).isEqualTo(1);
    }

    @Test
    void 같은_날은_허용한다() {
        assertThat(schedule("2026-11-20", "2026-11-20", "2026-11-20", 1, TODAY).items()).hasSize(16);
    }

    @Test
    void 입력이_규칙에_안_맞으면_문제_필드로_알려준다() {
        LocalDate d = LocalDate.of(2026, 11, 20);
        assertField(() -> service.getSchedule(null, d, null, null, TODAY), "contractDate");
        assertField(() -> service.getSchedule(d, null, null, null, TODAY), "balanceDate");
        assertField(() -> service.getSchedule(d, d.minusDays(1), null, null, TODAY), "balanceDate");
        assertField(() -> service.getSchedule(d, d, d.minusDays(1), null, TODAY), "moveInDate");
        assertField(() -> service.getSchedule(d, d, d, 0, TODAY), "termMonths");
    }

    @Test
    void 할_일_데이터가_16개가_아니면_일부만_돌려주지_않는다() {
        List<ChecklistItem> partial = ChecklistDataInitializer.defaults().stream()
                .filter(item -> item.getSortOrder() != 9 && item.getSortOrder() != 11).toList();
        when(repository.findAll()).thenReturn(partial);

        assertThatThrownBy(() -> schedule("2026-11-20", "2026-12-20", null, null, TODAY))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void 개수가_16개여도_순서가_겹치면_일부만_돌려주지_않는다() {
        List<ChecklistItem> duplicated = new ArrayList<>(ChecklistDataInitializer.defaults().subList(0, 15));
        duplicated.add(ChecklistDataInitializer.defaults().get(0));
        when(repository.findAll()).thenReturn(duplicated);

        assertThatThrownBy(() -> schedule("2026-11-20", "2026-12-20", null, null, TODAY))
                .isInstanceOf(IllegalStateException.class);
    }

    private static void assertField(Runnable call, String field) {
        assertThatThrownBy(call::run).isInstanceOfSatisfying(InvalidInputException.class,
                e -> assertThat(e.getField()).isEqualTo(field));
    }
}
