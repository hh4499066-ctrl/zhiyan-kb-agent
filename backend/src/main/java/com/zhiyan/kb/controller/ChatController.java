package com.zhiyan.kb.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import cn.hutool.core.util.IdUtil;
import com.zhiyan.kb.common.PageResult;
import com.zhiyan.kb.common.Result;
import com.zhiyan.kb.common.RoleNames;
import com.zhiyan.kb.common.StatusConstants;
import com.zhiyan.kb.common.UserContext;
import com.zhiyan.kb.dto.ChatAskRequest;
import com.zhiyan.kb.dto.ChatFeedbackRequest;
import com.zhiyan.kb.dto.CreateChatSessionRequest;
import com.zhiyan.kb.entity.ChatFeedback;
import com.zhiyan.kb.entity.ChatRecord;
import com.zhiyan.kb.entity.ChatSession;
import com.zhiyan.kb.mapper.ChatFeedbackMapper;
import com.zhiyan.kb.mapper.ChatRecordMapper;
import com.zhiyan.kb.mapper.ChatSessionMapper;
import com.zhiyan.kb.service.ChatService;
import com.zhiyan.kb.service.ResourceAccessService;
import com.zhiyan.kb.service.ShortTermMemoryService;
import com.zhiyan.kb.vo.ChatAskResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/chat")
public class ChatController {
    private final ChatService chatService;
    private final ChatSessionMapper sessionMapper;
    private final ChatRecordMapper recordMapper;
    private final ChatFeedbackMapper feedbackMapper;
    private final ShortTermMemoryService memoryService;
    private final ResourceAccessService accessService;

    public ChatController(ChatService chatService, ChatSessionMapper sessionMapper, ChatRecordMapper recordMapper,
                          ChatFeedbackMapper feedbackMapper, ShortTermMemoryService memoryService,
                          ResourceAccessService accessService) {
        this.chatService = chatService;
        this.sessionMapper = sessionMapper;
        this.recordMapper = recordMapper;
        this.feedbackMapper = feedbackMapper;
        this.memoryService = memoryService;
        this.accessService = accessService;
    }

    @PostMapping("/ask")
    public Result<ChatAskResponse> ask(@Valid @RequestBody ChatAskRequest request) {
        return Result.ok(chatService.ask(request));
    }

    @GetMapping("/sessions")
    public Result<PageResult<Map<String, Object>>> sessions(@RequestParam(defaultValue = "1") long page,
                                                            @RequestParam(defaultValue = "20") long size) {
        page = Math.max(1, page);
        size = Math.min(100, Math.max(1, size));
        com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<ChatRecord> query =
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<ChatRecord>()
                        .select("session_id AS sessionId",
                                "MAX(space_id) AS spaceId",
                                "MAX(create_time) AS updateTime",
                                "SUBSTRING_INDEX(GROUP_CONCAT(question ORDER BY create_time ASC SEPARATOR '\\n'), '\\n', 1) AS firstQuestion")
                        .groupBy("session_id")
                        .orderByDesc("MAX(create_time)");
        if (!RoleNames.ADMIN.equals(UserContext.role())) {
            query.eq("user_id", UserContext.userId());
        }
        Page<Map<String, Object>> result = recordMapper.selectMapsPage(Page.of(page, size), query);
        List<Map<String, Object>> sessions = result.getRecords().stream().map(row -> {
            Map<String, Object> session = new LinkedHashMap<>();
            session.put("sessionId", row.get("sessionId"));
            session.put("spaceId", row.get("spaceId"));
            session.put("updateTime", row.get("updateTime"));
            session.put("title", titleOf((String) row.get("firstQuestion")));
            return session;
        }).toList();
        return Result.ok(new PageResult<>(result.getTotal(), page, size, sessions));
    }

    private String titleOf(String question) {
        String title = question == null || question.isBlank() ? "New chat" : question.trim();
        return title.length() > 18 ? title.substring(0, 18) + "..." : title;
    }

    @PostMapping("/sessions")
    public Result<ChatSession> createSession(@Valid @RequestBody CreateChatSessionRequest request) {
        ChatSession session = new ChatSession();
        session.setId(IdUtil.fastSimpleUUID());
        session.setUserId(UserContext.userId());
        session.setSpaceId(request.getSpaceId());
        session.setStatus(StatusConstants.NORMAL);
        session.setTitle(request.getTitle() == null || request.getTitle().isBlank() ? "New chat" : request.getTitle());
        sessionMapper.insert(session);
        return Result.ok(session);
    }

    @DeleteMapping("/sessions/{id}")
    @Transactional
    public Result<Void> deleteSession(@PathVariable String id) {
        ChatSession session = sessionMapper.selectById(id);
        if (session != null && (RoleNames.ADMIN.equals(UserContext.role()) || UserContext.userId().equals(session.getUserId()))) {
            session.setStatus(StatusConstants.DELETED);
            sessionMapper.updateById(session);
        }
        recordMapper.delete(new LambdaQueryWrapper<ChatRecord>()
                .eq(ChatRecord::getSessionId, id)
                .eq(!RoleNames.ADMIN.equals(UserContext.role()), ChatRecord::getUserId, UserContext.userId()));
        memoryService.clearSession(id, UserContext.userId());
        return Result.ok();
    }

    @DeleteMapping("/sessions/{sessionId}/memory")
    public Result<Void> clearMemory(@PathVariable String sessionId) {
        memoryService.clearSession(sessionId, UserContext.userId());
        return Result.ok();
    }

    @GetMapping("/records")
    public Result<PageResult<ChatRecord>> records(@RequestParam(required = false) Long spaceId,
                                                  @RequestParam(required = false) String sessionId,
                                                  @RequestParam(defaultValue = "1") long page,
                                                  @RequestParam(defaultValue = "50") long size) {
        page = Math.max(1, page);
        size = Math.min(100, Math.max(1, size));
        LambdaQueryWrapper<ChatRecord> query = new LambdaQueryWrapper<ChatRecord>()
                .eq(spaceId != null, ChatRecord::getSpaceId, spaceId)
                .eq(sessionId != null && !sessionId.isBlank(), ChatRecord::getSessionId, sessionId);
        if (!RoleNames.ADMIN.equals(UserContext.role())) {
            query.eq(ChatRecord::getUserId, UserContext.userId());
        }
        if (sessionId != null && !sessionId.isBlank()) {
            query.orderByAsc(ChatRecord::getCreateTime);
        } else {
            query.orderByDesc(ChatRecord::getCreateTime);
        }
        Page<ChatRecord> result = recordMapper.selectPage(Page.of(page, size), query);
        return Result.ok(new PageResult<>(result.getTotal(), page, size, result.getRecords()));
    }

    @GetMapping("/records/{id}")
    public Result<ChatRecord> record(@PathVariable Long id) {
        return Result.ok(accessService.requireChatRecord(id));
    }

    @PostMapping("/records/{id}/feedback")
    public Result<ChatFeedback> feedback(@PathVariable Long id, @Valid @RequestBody ChatFeedbackRequest request) {
        accessService.requireChatRecord(id);
        ChatFeedback feedback = new ChatFeedback();
        feedback.setRecordId(id);
        feedback.setUserId(UserContext.userId());
        feedback.setHelpful(request.getHelpful());
        feedback.setComment(request.getComment());
        feedbackMapper.insert(feedback);
        return Result.ok(feedback);
    }

    @PutMapping("/records/{id}/favorite")
    public Result<Void> favorite(@PathVariable Long id, @RequestParam boolean favorite) {
        ChatRecord record = accessService.requireChatRecord(id);
        record.setFavorite(favorite);
        recordMapper.updateById(record);
        return Result.ok();
    }
}
