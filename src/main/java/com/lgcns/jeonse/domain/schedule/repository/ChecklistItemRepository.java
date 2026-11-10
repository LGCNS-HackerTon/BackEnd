package com.lgcns.jeonse.domain.schedule.repository;

import com.lgcns.jeonse.domain.schedule.entity.ChecklistItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface ChecklistItemRepository extends JpaRepository<ChecklistItem, Long> {
    /**
     * id를 직접 정해 할 일을 넣는다. db/seed.sql이 id = 순서 번호(1~16)로 넣고 다시 실행할 때 id로 덮어쓰므로,
     * 서버가 복구한 행도 같은 id여야 seed를 다시 실행해도 행이 늘지 않는다.
     */
    @Modifying
    @Transactional
    @Query(value = """
            INSERT INTO checklist_items (id, sort_order, anchor, offset_days, title, reason, important, link_name, link_url)
            VALUES (:id, :sortOrder, :anchor, :offsetDays, :title, :reason, :important, :linkName, :linkUrl)""",
            nativeQuery = true)
    void insertWithId(@Param("id") long id, @Param("sortOrder") int sortOrder, @Param("anchor") String anchor,
                      @Param("offsetDays") int offsetDays, @Param("title") String title, @Param("reason") String reason,
                      @Param("important") boolean important, @Param("linkName") String linkName,
                      @Param("linkUrl") String linkUrl);
}
