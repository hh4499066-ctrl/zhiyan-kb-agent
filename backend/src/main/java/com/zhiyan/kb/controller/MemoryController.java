package com.zhiyan.kb.controller;

import com.zhiyan.kb.common.Result;
import com.zhiyan.kb.common.UserContext;
import com.zhiyan.kb.entity.UserLongTermMemory;
import com.zhiyan.kb.mapper.UserLongTermMemoryMapper;
import com.zhiyan.kb.service.LongTermMemoryService;
import com.zhiyan.kb.service.ResourceAccessService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

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
    public Result<UserLongTermMemory> create(@RequestBody UserLongTermMemory memory) {
        memory.setUserId(UserContext.userId());
        memory.setStatus("NORMAL");
        memory.setEmbeddingText(memory.getContent());
        memory.setVectorId("mock-memory-" + System.currentTimeMillis());
        memoryMapper.insert(memory);
        return Result.ok(memory);
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody UserLongTermMemory memory) {
        accessService.requireOwnMemory(id);
        memory.setId(id);
        memory.setUserId(UserContext.userId());
        memory.setEmbeddingText(memory.getContent());
        memoryMapper.updateById(memory);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        accessService.requireOwnMemory(id);
        UserLongTermMemory memory = new UserLongTermMemory();
        memory.setId(id);
        memory.setStatus("DELETED");
        memoryMapper.updateById(memory);
        return Result.ok();
    }
}
