package com.zhiyan.kb.service;

import com.zhiyan.kb.entity.ChatRecord;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DashboardServiceTest {
    private final DashboardService dashboardService = new DashboardService();

    @Test
    void topQuestionsGroupsNormalizedSameQuestionAndSortsByCount() {
        List<Map<String, Object>> result = dashboardService.topQuestions(List.of(
                record("项目启动失败怎么办？", LocalDateTime.of(2026, 5, 7, 10, 0)),
                record(" 项目启动失败怎么办? ", LocalDateTime.of(2026, 5, 7, 11, 0)),
                record("项目启动失败怎么办。", LocalDateTime.of(2026, 5, 7, 12, 0)),
                record("Controller 可以写业务逻辑吗？", LocalDateTime.of(2026, 5, 7, 13, 0)),
                record("你知道我是谁吗？", LocalDateTime.of(2026, 5, 7, 14, 0)),
                record("你知道我是谁吗", LocalDateTime.of(2026, 5, 7, 15, 0))
        ));

        assertEquals(3L, result.get(0).get("count"));
        assertEquals("项目启动失败怎么办。", result.get(0).get("question"));
        assertEquals(2L, result.get(1).get("count"));
        assertEquals("你知道我是谁吗", result.get(1).get("question"));
        assertEquals(1L, result.get(2).get("count"));
    }

    private ChatRecord record(String question, LocalDateTime createTime) {
        ChatRecord record = new ChatRecord();
        record.setQuestion(question);
        record.setCreateTime(createTime);
        return record;
    }
}
