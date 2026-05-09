package com.zhiyan.kb.service;

import com.zhiyan.kb.common.BusinessException;
import com.zhiyan.kb.common.LoginUser;
import com.zhiyan.kb.common.RoleNames;
import com.zhiyan.kb.common.UserContext;
import com.zhiyan.kb.entity.ChatRecord;
import com.zhiyan.kb.entity.KbSpace;
import com.zhiyan.kb.entity.UserLongTermMemory;
import com.zhiyan.kb.mapper.ChatRecordMapper;
import com.zhiyan.kb.mapper.KbDocumentMapper;
import com.zhiyan.kb.mapper.KbSpaceMapper;
import com.zhiyan.kb.mapper.KbSpaceMemberMapper;
import com.zhiyan.kb.mapper.UnresolvedQuestionMapper;
import com.zhiyan.kb.mapper.UserLongTermMemoryMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ResourceAccessServiceTest {
    private final ChatRecordMapper chatRecordMapper = mock(ChatRecordMapper.class);
    private final UserLongTermMemoryMapper memoryMapper = mock(UserLongTermMemoryMapper.class);
    private final KbDocumentMapper documentMapper = mock(KbDocumentMapper.class);
    private final KbSpaceMapper spaceMapper = mock(KbSpaceMapper.class);
    private final KbSpaceMemberMapper spaceMemberMapper = mock(KbSpaceMemberMapper.class);
    private final UnresolvedQuestionMapper unresolvedQuestionMapper = mock(UnresolvedQuestionMapper.class);
    private final ResourceAccessService service = new ResourceAccessService(
            chatRecordMapper, memoryMapper, documentMapper, spaceMapper, spaceMemberMapper, unresolvedQuestionMapper);

    @AfterEach
    void clearUserContext() {
        UserContext.clear();
    }

    @Test
    void employeeCannotReadAnotherUsersChatRecord() {
        login(1L, RoleNames.EMPLOYEE);
        ChatRecord record = new ChatRecord();
        record.setId(10L);
        record.setUserId(2L);
        when(chatRecordMapper.selectById(10L)).thenReturn(record);

        assertThatThrownBy(() -> service.requireChatRecord(10L))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo(403);
    }

    @Test
    void employeeCannotModifyAnotherUsersMemory() {
        login(1L, RoleNames.EMPLOYEE);
        UserLongTermMemory memory = new UserLongTermMemory();
        memory.setId(10L);
        memory.setUserId(2L);
        memory.setStatus("NORMAL");
        when(memoryMapper.selectById(10L)).thenReturn(memory);

        assertThatThrownBy(() -> service.requireOwnMemory(10L))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo(403);
    }

    @Test
    void privateSpaceCanOnlyBeAccessedByOwnerOrMember() {
        login(1L, RoleNames.EMPLOYEE);
        KbSpace space = privateSpace(10L, 2L);
        when(spaceMapper.selectById(10L)).thenReturn(space);
        when(spaceMemberMapper.selectCount(any())).thenReturn(0L);

        assertThat(service.canAccessSpace(10L)).isFalse();

        space.setOwnerId(1L);
        assertThat(service.canAccessSpace(10L)).isTrue();

        space.setOwnerId(2L);
        when(spaceMemberMapper.selectCount(any())).thenReturn(1L);
        assertThat(service.canAccessSpace(10L)).isTrue();
    }

    @Test
    void managerMemberCanManageSpace() {
        login(1L, RoleNames.EMPLOYEE);
        when(spaceMapper.selectById(10L)).thenReturn(privateSpace(10L, 2L));
        when(spaceMemberMapper.selectCount(any())).thenReturn(1L);

        assertThatCode(() -> service.requireSpaceManage(10L)).doesNotThrowAnyException();
    }

    @Test
    void nonManagerMemberCanReadButCannotManagePrivateSpace() {
        login(1L, RoleNames.EMPLOYEE);
        when(spaceMapper.selectById(10L)).thenReturn(privateSpace(10L, 2L));
        when(spaceMemberMapper.selectCount(any())).thenReturn(1L, 0L);

        assertThat(service.canAccessSpace(10L)).isTrue();
        assertThatThrownBy(() -> service.requireSpaceManage(10L))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo(403);
    }

    private KbSpace privateSpace(Long id, Long ownerId) {
        KbSpace space = new KbSpace();
        space.setId(id);
        space.setOwnerId(ownerId);
        space.setVisibility("PRIVATE");
        space.setStatus("NORMAL");
        return space;
    }

    private void login(Long userId, String role) {
        LoginUser user = new LoginUser();
        user.setId(userId);
        user.setRole(role);
        UserContext.set(user);
    }
}
