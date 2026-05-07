package com.zhiyan.kb.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.zhiyan.kb.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@TableName("onboarding_plan")
@EqualsAndHashCode(callSuper = true)
public class OnboardingPlan extends BaseEntity {
    @TableId
    private Long id;
    private Long userId;
    private String roleType;
    private String title;
    private String description;
    private String planContent;
    private String recommendedDocuments;
}
