package com.zhiyan.kb.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhiyan.kb.common.BusinessException;
import com.zhiyan.kb.common.RoleNames;
import com.zhiyan.kb.common.StatusConstants;
import com.zhiyan.kb.common.UserContext;
import com.zhiyan.kb.entity.ChatRecord;
import com.zhiyan.kb.entity.KbDocument;
import com.zhiyan.kb.entity.KbSpace;
import com.zhiyan.kb.entity.KbSpaceMember;
import com.zhiyan.kb.entity.UnresolvedQuestion;
import com.zhiyan.kb.entity.UserLongTermMemory;
import com.zhiyan.kb.mapper.ChatRecordMapper;
import com.zhiyan.kb.mapper.KbDocumentMapper;
import com.zhiyan.kb.mapper.KbSpaceMapper;
import com.zhiyan.kb.mapper.KbSpaceMemberMapper;
import com.zhiyan.kb.mapper.UnresolvedQuestionMapper;
import com.zhiyan.kb.mapper.UserLongTermMemoryMapper;
import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Service
public class ResourceAccessService {
    private final ChatRecordMapper chatRecordMapper;
    private final UserLongTermMemoryMapper memoryMapper;
    private final KbDocumentMapper documentMapper;
    private final KbSpaceMapper spaceMapper;
    private final KbSpaceMemberMapper spaceMemberMapper;
    private final UnresolvedQuestionMapper unresolvedQuestionMapper;

    public ResourceAccessService(ChatRecordMapper chatRecordMapper, UserLongTermMemoryMapper memoryMapper,
                                 KbDocumentMapper documentMapper, KbSpaceMapper spaceMapper,
                                 KbSpaceMemberMapper spaceMemberMapper, UnresolvedQuestionMapper unresolvedQuestionMapper) {
        this.chatRecordMapper = chatRecordMapper;
        this.memoryMapper = memoryMapper;
        this.documentMapper = documentMapper;
        this.spaceMapper = spaceMapper;
        this.spaceMemberMapper = spaceMemberMapper;
        this.unresolvedQuestionMapper = unresolvedQuestionMapper;
    }

    public boolean isAdmin() {
        return RoleNames.ADMIN.equals(UserContext.role());
    }

    public boolean isKbManager() {
        return RoleNames.KB_MANAGER.equals(UserContext.role());
    }

    public boolean canAccessSpace(Long spaceId) {
        if (spaceId == null) {
            return false;
        }
        KbSpace space = spaceMapper.selectById(spaceId);
        if (space == null || StatusConstants.DELETED.equals(space.getStatus())) {
            return false;
        }
        if (isAdmin() || isKbManager()) {
            return true;
        }
        Long userId = UserContext.userId();
        if (Objects.equals(space.getOwnerId(), userId) || "PUBLIC".equalsIgnoreCase(space.getVisibility())) {
            return true;
        }
        Long count = spaceMemberMapper.selectCount(new LambdaQueryWrapper<KbSpaceMember>()
                .eq(KbSpaceMember::getSpaceId, spaceId)
                .eq(KbSpaceMember::getUserId, userId)
                .eq(KbSpaceMember::getStatus, StatusConstants.NORMAL));
        return count != null && count > 0;
    }

    public void requireSpaceAccess(Long spaceId) {
        if (!canAccessSpace(spaceId)) {
            throw new BusinessException(403, "No permission to access this space");
        }
    }

    public void requireSpaceManage(Long spaceId) {
        KbSpace space = spaceMapper.selectById(spaceId);
        if (space == null || StatusConstants.DELETED.equals(space.getStatus())) {
            throw new BusinessException(404, "Space not found");
        }
        if (isAdmin() || isKbManager()) {
            return;
        }
        Long userId = UserContext.userId();
        if (Objects.equals(space.getOwnerId(), userId)) {
            return;
        }
        Long count = spaceMemberMapper.selectCount(new LambdaQueryWrapper<KbSpaceMember>()
                .eq(KbSpaceMember::getSpaceId, spaceId)
                .eq(KbSpaceMember::getUserId, userId)
                .eq(KbSpaceMember::getMemberRole, "MANAGER")
                .eq(KbSpaceMember::getStatus, StatusConstants.NORMAL));
        if (count == null || count == 0) {
            throw new BusinessException(403, "No permission to manage this space");
        }
    }

    public List<Long> accessibleNormalSpaceIds() {
        if (isAdmin() || isKbManager()) {
            return spaceMapper.selectList(new LambdaQueryWrapper<KbSpace>()
                            .eq(KbSpace::getStatus, StatusConstants.NORMAL))
                    .stream()
                    .map(KbSpace::getId)
                    .toList();
        }
        Long userId = UserContext.userId();
        Set<Long> ids = new LinkedHashSet<>();
        spaceMapper.selectList(new LambdaQueryWrapper<KbSpace>()
                        .eq(KbSpace::getStatus, StatusConstants.NORMAL)
                        .and(q -> q.eq(KbSpace::getVisibility, "PUBLIC")
                                .or()
                                .eq(KbSpace::getOwnerId, userId)))
                .forEach(space -> ids.add(space.getId()));
        spaceMemberMapper.selectList(new LambdaQueryWrapper<KbSpaceMember>()
                        .eq(KbSpaceMember::getUserId, userId)
                        .eq(KbSpaceMember::getStatus, StatusConstants.NORMAL))
                .stream()
                .map(KbSpaceMember::getSpaceId)
                .distinct()
                .toList()
                .stream()
                .filter(memberSpaceId -> !ids.contains(memberSpaceId))
                .toList()
                .forEach(ids::add);
        if (!ids.isEmpty()) {
            Set<Long> normalIds = spaceMapper.selectList(new LambdaQueryWrapper<KbSpace>()
                            .in(KbSpace::getId, ids)
                            .eq(KbSpace::getStatus, StatusConstants.NORMAL))
                    .stream()
                    .map(KbSpace::getId)
                    .collect(java.util.stream.Collectors.toSet());
            ids.removeIf(id -> !normalIds.contains(id));
        }
        return List.copyOf(ids);
    }

    public ChatRecord requireChatRecord(Long id) {
        ChatRecord record = chatRecordMapper.selectById(id);
        if (record == null) {
            throw new BusinessException(404, "Chat record not found");
        }
        if (!isAdmin() && !Objects.equals(record.getUserId(), UserContext.userId())) {
            throw new BusinessException(403, "No permission to access this chat record");
        }
        return record;
    }

    public UserLongTermMemory requireOwnMemory(Long id) {
        UserLongTermMemory memory = memoryMapper.selectById(id);
        if (memory == null || StatusConstants.DELETED.equals(memory.getStatus())) {
            throw new BusinessException(404, "Memory not found");
        }
        if (!Objects.equals(memory.getUserId(), UserContext.userId())) {
            throw new BusinessException(403, "No permission to access this memory");
        }
        return memory;
    }

    public KbDocument requireDocument(Long id) {
        KbDocument document = documentMapper.selectById(id);
        if (document == null || StatusConstants.DELETED.equals(document.getStatus())) {
            throw new BusinessException(404, "Document not found");
        }
        requireSpaceAccess(document.getSpaceId());
        return document;
    }

    public KbDocument requireDocumentManage(Long id) {
        KbDocument document = documentMapper.selectById(id);
        if (document == null || StatusConstants.DELETED.equals(document.getStatus())) {
            throw new BusinessException(404, "Document not found");
        }
        requireSpaceManage(document.getSpaceId());
        return document;
    }

    public UnresolvedQuestion requireUnresolvedQuestionManage(Long id) {
        UnresolvedQuestion question = unresolvedQuestionMapper.selectById(id);
        if (question == null) {
            throw new BusinessException(404, "Unresolved question not found");
        }
        requireSpaceManage(question.getSpaceId());
        return question;
    }
}
