package com.zhiyan.kb.controller;

import com.zhiyan.kb.common.StatusConstants;
import com.zhiyan.kb.common.Result;
import com.zhiyan.kb.common.UserContext;
import com.zhiyan.kb.dto.CreateMemoryRequest;
import com.zhiyan.kb.dto.UpdateMemoryRequest;
import com.zhiyan.kb.entity.UserLongTermMemory;
import com.zhiyan.kb.mapper.UserLongTermMemoryMapper;
import com.zhiyan.kb.service.LongTermMemoryService;
import com.zhiyan.kb.service.ResourceAccessService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/memories")
public class MemoryController {
    private final LongTermMemoryService memoryService;
    private final UserLongTermMemoryMapper memoryMapper;
    private final ResourceAccessService accessService;

    public MemoryController(LongTermMemoryService memoryService, UserLongTermMemoryMapper memoryMapper,
                            ResourceAccessService accessService) {
        this.memoryService = memoryService;
        this.memoryMapper = memoryMapper;
        this.accessService = accessService;
    }

    @GetMapping
    public Result<List<UserLongTermMemory>> list() {
        return Result.ok(memoryService.list(UserContext.userId()));
    }

    @PostMapping
    public Result<UserLongTermMemory> create(@Valid @RequestBody CreateMemoryRequest request) {
        UserLongTermMemory memory = new UserLongTermMemory();
        memory.setUserId(UserContext.userId());
        memory.setMemoryType(request.getMemoryType());
        memory.setContent(request.getContent());
        memory.setStatus(StatusConstants.NORMAL);
        memory.setEmbeddingText(request.getContent());
        memory.setVectorId("memory-" + UUID.randomUUID());
        memoryMapper.insert(memory);
        return Result.ok(memory);
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody UpdateMemoryRequest request) {
        accessService.requireOwnMemory(id);
        UserLongTermMemory update = new UserLongTermMemory();
        update.setId(id);
        update.setMemoryType(request.getMemoryType());
        update.setContent(request.getContent());
        update.setEmbeddingText(request.getContent());
        memoryMapper.updateById(update);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        accessService.requireOwnMemory(id);
        UserLongTermMemory memory = new UserLongTermMemory();
        memory.setId(id);
        memory.setStatus(StatusConstants.DELETED);
        memoryMapper.updateById(memory);
        return Result.ok();
    }
}
