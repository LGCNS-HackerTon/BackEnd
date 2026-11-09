package com.lgcns.jeonse.domain.schedule.repository;

import com.lgcns.jeonse.domain.schedule.entity.ChecklistItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChecklistItemRepository extends JpaRepository<ChecklistItem, Long> { }
