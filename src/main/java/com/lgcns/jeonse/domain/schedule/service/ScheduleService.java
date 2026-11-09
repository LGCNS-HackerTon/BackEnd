package com.lgcns.jeonse.domain.schedule.service;

import com.lgcns.jeonse.domain.schedule.dto.ScheduleResponseDTO;
import com.lgcns.jeonse.domain.schedule.dto.ScheduleResponseDTO.Link;
import com.lgcns.jeonse.domain.schedule.dto.ScheduleResponseDTO.ScheduleItem;
import com.lgcns.jeonse.domain.schedule.repository.ChecklistItemRepository;
import com.lgcns.jeonse.global.exception.InvalidInputException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ScheduleService {
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final int DEFAULT_TERM_MONTHS = 24;
    private final ChecklistItemRepository repository;

    public ScheduleService(ChecklistItemRepository repository) { this.repository = repository; }

    public ScheduleResponseDTO getSchedule(LocalDate contractDate, LocalDate balanceDate, LocalDate moveInDate,
                                           Integer termMonths) {
        return getSchedule(contractDate, balanceDate, moveInDate, termMonths, LocalDate.now(KST));
    }

    ScheduleResponseDTO getSchedule(LocalDate contractDate, LocalDate balanceDate, LocalDate moveInDate,
                                    Integer termMonths, LocalDate today) {
        if (contractDate == null) throw new InvalidInputException("계약일을 입력해 주세요.", "contractDate");
        if (balanceDate == null) throw new InvalidInputException("잔금일을 입력해 주세요.", "balanceDate");
        if (balanceDate.isBefore(contractDate))
            throw new InvalidInputException("잔금일은 계약일과 같거나 그 뒤여야 해요.", "balanceDate");
        LocalDate moveIn = moveInDate == null ? balanceDate : moveInDate;
        if (moveIn.isBefore(balanceDate))
            throw new InvalidInputException("입주일은 잔금일과 같거나 그 뒤여야 해요.", "moveInDate");
        int term = termMonths == null ? DEFAULT_TERM_MONTHS : termMonths;
        if (term < 1) throw new InvalidInputException("계약 기간은 1개월 이상이어야 해요.", "termMonths");

        // plusMonths는 없는 날(예: 2월 31일)을 그 달 마지막 날로 맞춘다 (FE mock과 같음)
        LocalDate expiryDate = moveIn.plusMonths(term);
        Map<String, LocalDate> anchors = Map.of(
                "CONTRACT", contractDate, "BALANCE", balanceDate, "MOVE_IN", moveIn, "EXPIRY", expiryDate);

        List<ScheduleItem> items = repository.findAll().stream()
                .map(item -> {
                    LocalDate date = anchors.get(item.getAnchor()).plusDays(item.getOffsetDays());
                    int daysLeft = (int) ChronoUnit.DAYS.between(today, date);
                    String status = daysLeft < 0 ? "PAST" : daysLeft == 0 ? "TODAY" : "UPCOMING";
                    Link link = item.getLinkName() == null ? null : new Link(item.getLinkName(), item.getLinkUrl());
                    return new ScheduleItem(item.getSortOrder(), item.getAnchor(), date, daysLeft, status,
                            item.getTitle(), item.getReason(), item.isImportant(), link);
                })
                .sorted(Comparator.comparing(ScheduleItem::date).thenComparingInt(ScheduleItem::order))
                .toList();
        return new ScheduleResponseDTO(today, expiryDate, items);
    }
}
