package com.zhiyan.kb.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.zhiyan.kb.common.Result;
import com.zhiyan.kb.common.StatusConstants;
import com.zhiyan.kb.entity.ChatFeedback;
import com.zhiyan.kb.entity.ChatRecord;
import com.zhiyan.kb.entity.KbDocument;
import com.zhiyan.kb.entity.KbDocumentChunk;
import com.zhiyan.kb.entity.KbSpace;
import com.zhiyan.kb.entity.SysDepartment;
import com.zhiyan.kb.entity.SysUser;
import com.zhiyan.kb.entity.UnresolvedQuestion;
import com.zhiyan.kb.mapper.ChatFeedbackMapper;
import com.zhiyan.kb.mapper.ChatRecordMapper;
import com.zhiyan.kb.mapper.KbDocumentChunkMapper;
import com.zhiyan.kb.mapper.KbDocumentMapper;
import com.zhiyan.kb.mapper.KbSpaceMapper;
import com.zhiyan.kb.mapper.SysDepartmentMapper;
import com.zhiyan.kb.mapper.SysUserMapper;
import com.zhiyan.kb.mapper.UnresolvedQuestionMapper;
import com.zhiyan.kb.service.DashboardService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {
    private final KbSpaceMapper spaceMapper;
    private final KbDocumentMapper documentMapper;
    private final KbDocumentChunkMapper chunkMapper;
    private final ChatRecordMapper recordMapper;
    private final UnresolvedQuestionMapper unresolvedMapper;
    private final ChatFeedbackMapper feedbackMapper;
    private final SysUserMapper userMapper;
    private final SysDepartmentMapper departmentMapper;
    private final DashboardService dashboardService;

    public DashboardController(KbSpaceMapper spaceMapper, KbDocumentMapper documentMapper, KbDocumentChunkMapper chunkMapper,
                               ChatRecordMapper recordMapper, UnresolvedQuestionMapper unresolvedMapper,
                               ChatFeedbackMapper feedbackMapper, SysUserMapper userMapper,
                               SysDepartmentMapper departmentMapper, DashboardService dashboardService) {
        this.spaceMapper = spaceMapper;
        this.documentMapper = documentMapper;
        this.chunkMapper = chunkMapper;
        this.recordMapper = recordMapper;
        this.unresolvedMapper = unresolvedMapper;
        this.feedbackMapper = feedbackMapper;
        this.userMapper = userMapper;
        this.departmentMapper = departmentMapper;
        this.dashboardService = dashboardService;
    }

    @GetMapping("/overview")
    public Result<Map<String, Object>> overview() {
        long qaCount = recordMapper.selectCount(null);
        long helpful = feedbackMapper.selectCount(new LambdaQueryWrapper<ChatFeedback>().eq(ChatFeedback::getHelpful, true));
        long feedback = feedbackMapper.selectCount(null);
        List<ChatRecord> topQuestionRecords = recordMapper.selectPage(
                com.baomidou.mybatisplus.extension.plugins.pagination.Page.of(1, 200),
                new LambdaQueryWrapper<ChatRecord>()
                        .select(ChatRecord::getQuestion, ChatRecord::getCreateTime)
                        .isNotNull(ChatRecord::getQuestion)
                        .orderByDesc(ChatRecord::getCreateTime)).getRecords();

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("spaceCount", spaceMapper.selectCount(new LambdaQueryWrapper<KbSpace>().ne(KbSpace::getStatus, StatusConstants.DELETED)));
        data.put("documentCount", documentMapper.selectCount(new LambdaQueryWrapper<KbDocument>().eq(KbDocument::getStatus, StatusConstants.NORMAL)));
        data.put("chunkCount", chunkMapper.selectCount(new LambdaQueryWrapper<KbDocumentChunk>().eq(KbDocumentChunk::getStatus, StatusConstants.NORMAL)));
        data.put("qaCount", qaCount);
        data.put("todayQaCount", recordMapper.selectCount(new LambdaQueryWrapper<ChatRecord>().ge(ChatRecord::getCreateTime, LocalDate.now().atStartOfDay())));
        data.put("unresolvedCount", unresolvedMapper.selectCount(new LambdaQueryWrapper<UnresolvedQuestion>().eq(UnresolvedQuestion::getStatus, StatusConstants.PENDING)));
        data.put("topQuestions", dashboardService.topQuestions(topQuestionRecords));
        data.put("qaTrend", groupedByDate("chat_record"));
        data.put("documentTrend", groupedByDate("kb_document"));
        data.put("departmentContribution", departmentContribution());
        data.put("satisfactionRate", feedback == 0 ? 100 : Math.round(helpful * 100.0 / feedback));
        return Result.ok(data);
    }

    private List<Map<String, Object>> groupedByDate(String tableName) {
        if ("chat_record".equals(tableName)) {
            return recordMapper.selectMaps(new QueryWrapper<ChatRecord>()
                    .select("DATE(create_time) AS date", "COUNT(*) AS count")
                    .ge("create_time", LocalDate.now().minusDays(6).atStartOfDay())
                    .groupBy("DATE(create_time)")
                    .orderByAsc("date"));
        }
        return documentMapper.selectMaps(new QueryWrapper<KbDocument>()
                .select("DATE(create_time) AS date", "COUNT(*) AS count")
                .ge("create_time", LocalDate.now().minusDays(6).atStartOfDay())
                .groupBy("DATE(create_time)")
                .orderByAsc("date"));
    }

    private List<Map<String, Object>> departmentContribution() {
        List<Map<String, Object>> userCounts = recordMapper.selectMaps(new QueryWrapper<ChatRecord>()
                .select("user_id AS userId", "COUNT(*) AS count")
                .groupBy("user_id")
                .orderByDesc("count")
                .last("LIMIT 100"));
        if (userCounts.isEmpty()) {
            return List.of();
        }
        List<Long> userIds = userCounts.stream()
                .map(row -> ((Number) row.get("userId")).longValue())
                .distinct()
                .toList();
        Map<Long, SysUser> users = userMapper.selectList(new LambdaQueryWrapper<SysUser>().in(SysUser::getId, userIds))
                .stream()
                .collect(Collectors.toMap(SysUser::getId, Function.identity()));
        List<Long> departmentIds = users.values().stream()
                .map(SysUser::getDepartmentId)
                .filter(id -> id != null && id > 0)
                .distinct()
                .toList();
        Map<Long, String> departments = departmentIds.isEmpty() ? Map.of()
                : departmentMapper.selectList(new LambdaQueryWrapper<SysDepartment>().in(SysDepartment::getId, departmentIds))
                .stream()
                .collect(Collectors.toMap(SysDepartment::getId, SysDepartment::getName));
        Map<String, Long> totals = new LinkedHashMap<>();
        for (Map<String, Object> row : userCounts) {
            SysUser user = users.get(((Number) row.get("userId")).longValue());
            String name = user == null ? "Unknown" : departments.getOrDefault(user.getDepartmentId(), "Unknown");
            totals.merge(name, ((Number) row.get("count")).longValue(), Long::sum);
        }
        return totals.entrySet().stream()
                .map(entry -> Map.<String, Object>of("name", entry.getKey(), "count", entry.getValue()))
                .toList();
    }
}
