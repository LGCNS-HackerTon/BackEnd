package com.lgcns.jeonse.domain.schedule.dto;

import java.time.LocalDate;
import java.util.List;

public record ScheduleResponseDTO(LocalDate today, LocalDate expiryDate, List<ScheduleItem> items) {
    public record ScheduleItem(int order, String anchor, LocalDate date, int daysLeft, String status,
                               String title, String reason, boolean important, Link link) { }

    public record Link(String name, String url) { }
}
