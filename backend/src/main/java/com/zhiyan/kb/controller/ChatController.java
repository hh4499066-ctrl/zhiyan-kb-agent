package com.zhiyan.kb.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhiyan.kb.common.Result;
import com.zhiyan.kb.common.UserContext;
import com.zhiyan.kb.dto.ChatAskRequest;
import com.zhiyan.kb.entity.ChatFeedback;
import com.zhiyan.kb.entity.ChatRecord;
import com.zhiyan.kb.entity.ChatSession;
import com.zhiyan.kb.mapper.ChatFeedbackMapper;
import com.zhiyan.kb.mapper.ChatRecordMapper;
import com.zhiyan.kb.mapper.ChatSessionMapper;
import com.zhiyan.kb.service.ChatService;
import com.zhiyan.kb.service.ShortTermMemoryService;
import com.zhiyan.kb.vo.ChatAskResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

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

    public ChatController(ChatService chatService, ChatSessionMapper sessionMapper, ChatRecordMapper recordMapper,
                          ChatFeedbackMapper feedbackMapper, ShortTermMemoryService memoryService) {
        this.chatService = chatService;
        this.sessionMapper = sessionMapper;
        this.recordMapper = recordMapper;
        this.feedbackMapper = feedbackMapper;
        this.memoryService = memoryService;
    }

    @PostMapping("/ask")
    public Result<ChatAskResponse> ask(@Valid @RequestBody ChatAskRequest request) {
        return Result.ok(chatService.ask(request));
    }

    @GetMapping("/sessions")
    public Result<List<Map<String, Object>>> sessions() {
        LambdaQueryWrapper<ChatRecord> qw = new LambdaQueryWrapper<ChatRecord>().orderByDesc(ChatRecord::getCreateTime);
        if (!"admin".equals(UserContext.role())) {
            qw.eq(ChatRecord::getUserId, UserContext.userId());
        }
        List<ChatRecord> records = recordMapper.selectList(qw);
        Map<String, Map<String, Object>> grouped = new LinkedHashMap<>();
        for (ChatRecord record : records) {
            grouped.computeIfAbsent(record.getSessionId(), sessionId -> {
                Map<String, Object> session = new LinkedHashMap<>();
                session.put("sessionId", sessionId);
                session.put("title", record.getQuestion().length() > 18 ? record.getQuestion().substring(0, 18) + "..." : record.getQuestion());
                session.put("spaceId", record.getSpaceId());
                session.put("updateTime", record.getCreateTime());
                return session;
            });
        }
        return Result.ok(new ArrayList<>(grouped.values()));
    }

    @PostMapping("/sessions")
    public Result<ChatSession> createSession(@RequestBody ChatSession session) {
        session.setUserId(UserContext.userId());
        session.setStatus("NORMAL");
        session.setTitle(session.getTitle() == null ? "新会话" : session.getTitle());
        sessionMapper.insert(session);
        return Result.ok(session);
    }

    @DeleteMapping("/sessions/{id}")
    public Result<Void> deleteSession(@PathVariable String id) {
        if (id.matches("\\d+")) {
            ChatSession session = sessionMapper.selectById(Long.valueOf(id));
            if (session != null) {
                session.setStatus("DELETED");
                sessionMapper.updateById(session);
            }
        }
        recordMapper.delete(new LambdaQueryWrapper<ChatRecord>()
                .eq(ChatRecord::getSessionId, id)
                .eq(!"admin".equals(UserContext.role()), ChatRecord::getUserId, UserContext.userId()));
        memoryService.clearSession(id, UserContext.userId());
        return Result.ok();
    }

    @DeleteMapping("/sessions/{sessionId}/memory")
    public Result<Void> clearMemory(@PathVariable String sessionId) {
        memoryService.clearSession(sessionId, UserContext.userId());
        return Result.ok();
    }

    @GetMapping("/records")
    public Result<List<ChatRecord>> records(@RequestParam(required = false) Long spaceId, @RequestParam(required = false) String sessionId) {
        LambdaQueryWrapper<ChatRecord> qw = new LambdaQueryWrapper<ChatRecord>()
                .eq(spaceId != null, ChatRecord::getSpaceId, spaceId)
                .eq(sessionId != null && !sessionId.isBlank(), ChatRecord::getSessionId, sessionId);
        if (!"admin".equals(UserContext.role())) {
            qw.eq(ChatRecord::getUserId, UserContext.userId());
        }
        if (sessionId != null && !sessionId.isBlank()) {
            qw.orderByAsc(ChatRecord::getCreateTime);
        } else {
            qw.orderByDesc(ChatRecord::getCreateTime);
        }
        return Result.ok(recordMapper.selectList(qw));
    }

    @GetMapping("/records/{id}")
    public Result<ChatRecord> record(@PathVariable Long id) {
        return Result.ok(recordMapper.selectById(id));
    }

    @PostMapping("/records/{id}/feedback")
    public Result<ChatFeedback> feedback(@PathVariable Long id, @RequestBody ChatFeedback feedback) {
        feedback.setRecordId(id);
        feedback.setUserId(UserContext.userId());
        feedbackMapper.insert(feedback);
        return Result.ok(feedback);
    }

    @PutMapping("/records/{id}/favorite")
    public Result<Void> favorite(@PathVariable Long id, @RequestParam boolean favorite) {
        ChatRecord record = new ChatRecord();
        record.setId(id);
        record.setFavorite(favorite);
        recordMapper.updateById(record);
        return Result.ok();
    }
}
