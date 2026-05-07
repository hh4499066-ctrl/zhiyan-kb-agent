package com.zhiyan.kb.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhiyan.kb.common.Result;
import com.zhiyan.kb.entity.*;
import com.zhiyan.kb.mapper.*;
import com.zhiyan.kb.service.DashboardService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {
    private final KbSpaceMapper spaceMapper;
    private final KbDocumentMapper documentMapper;
    private final KbDocumentChunkMapper chunkMapper;
    private final ChatRecordMapper recordMapper;
    private final UnresolvedQuestionMapper unresolvedMapper;
    private final ChatFeedbackMapper feedbackMapper;
    private final DashboardService dashboardService;

    public DashboardController(KbSpaceMapper spaceMapper, KbDocumentMapper documentMapper, KbDocumentChunkMapper chunkMapper,
                               ChatRecordMapper recordMapper, UnresolvedQuestionMapper unresolvedMapper, ChatFeedbackMapper feedbackMapper,
                               DashboardService dashboardService) {
        this.spaceMapper = spaceMapper;
        this.documentMapper = documentMapper;
        this.chunkMapper = chunkMapper;
        this.recordMapper = recordMapper;
        this.unresolvedMapper = unresolvedMapper;
        this.feedbackMapper = feedbackMapper;
        this.dashboardService = dashboardService;
    }

    @GetMapping("/overview")
    public Result<Map<String, Object>> overview() {
        long qaCount = recordMapper.selectCount(null);
        long helpful = feedbackMapper.selectCount(new LambdaQueryWrapper<ChatFeedback>().eq(ChatFeedback::getHelpful, true));
        long feedback = feedbackMapper.selectCount(null);
        List<ChatRecord> topQuestionRecords = recordMapper.selectList(new LambdaQueryWrapper<ChatRecord>()
                .select(ChatRecord::getQuestion, ChatRecord::getCreateTime)
                .isNotNull(ChatRecord::getQuestion)
                .orderByDesc(ChatRecord::getCreateTime));
        List<Map<String, Object>> topQuestions = dashboardService.topQuestions(topQuestionRecords);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("spaceCount", spaceMapper.selectCount(new LambdaQueryWrapper<KbSpace>().ne(KbSpace::getStatus, "DELETED")));
        data.put("documentCount", documentMapper.selectCount(new LambdaQueryWrapper<KbDocument>().eq(KbDocument::getStatus, "NORMAL")));
        data.put("chunkCount", chunkMapper.selectCount(new LambdaQueryWrapper<KbDocumentChunk>().eq(KbDocumentChunk::getStatus, "NORMAL")));
        data.put("qaCount", qaCount);
        data.put("todayQaCount", recordMapper.selectCount(new LambdaQueryWrapper<ChatRecord>().ge(ChatRecord::getCreateTime, LocalDate.now().atStartOfDay())));
        data.put("unresolvedCount", unresolvedMapper.selectCount(new LambdaQueryWrapper<UnresolvedQuestion>().eq(UnresolvedQuestion::getStatus, "PENDING")));
        data.put("topQuestions", topQuestions);
        data.put("qaTrend", List.of(Map.of("date", "周一", "count", 12), Map.of("date", "周二", "count", 18), Map.of("date", "周三", "count", qaCount)));
        data.put("documentTrend", List.of(Map.of("date", "周一", "count", 2), Map.of("date", "周二", "count", 5), Map.of("date", "周三", "count", 8)));
        data.put("departmentContribution", List.of(Map.of("name", "研发部", "count", 8), Map.of("name", "测试部", "count", 3), Map.of("name", "运维部", "count", 4)));
        data.put("satisfactionRate", feedback == 0 ? 100 : Math.round(helpful * 100.0 / feedback));
        return Result.ok(data);
    }
}
