package com.lgcns.jeonse.domain.schedule.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "checklist_items")
@Getter
@NoArgsConstructor
public class ChecklistItem {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "sort_order", nullable = false, unique = true) private int sortOrder;
    @Column(nullable = false, length = 10) private String anchor;
    @Column(name = "offset_days", nullable = false) private int offsetDays;
    @Column(nullable = false, length = 100) private String title;
    @Column(nullable = false) private String reason;
    @Column(nullable = false) private boolean important;
    @Column(name = "link_name", length = 50) private String linkName;
    @Column(name = "link_url") private String linkUrl;

    public ChecklistItem(int sortOrder, String anchor, int offsetDays, String title, String reason,
                         boolean important, String linkName, String linkUrl) {
        this.sortOrder = sortOrder; this.anchor = anchor; this.offsetDays = offsetDays;
        this.title = title; this.reason = reason; this.important = important;
        this.linkName = linkName; this.linkUrl = linkUrl;
    }
}
