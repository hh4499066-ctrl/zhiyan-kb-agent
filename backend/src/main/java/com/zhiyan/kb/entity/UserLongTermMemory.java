package com.zhiyan.kb.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.zhiyan.kb.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@TableName("user_long_term_memory")
@EqualsAndHashCode(callSuper = true)
public class UserLongTermMemory extends BaseEntity {
    @TableId
    private Long id;
    private Long userId;
    private String memoryType;
    private String content;
    private String embeddingText;
    private String vectorId;
    private String status;
}
