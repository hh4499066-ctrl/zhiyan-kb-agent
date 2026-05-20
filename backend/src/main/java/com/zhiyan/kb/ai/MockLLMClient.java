package com.zhiyan.kb.ai;

import com.zhiyan.kb.dto.ChatMemoryMessage;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class MockLLMClient implements LLMClient {
    @Override
    public String complete(String prompt) {
        if (prompt == null) {
            return "当前知识库中没有找到明确答案，已记录为未解决问题。";
        }
        if (prompt.contains("未检索到相关知识库片段")) {
            return "未检索到相关知识库来源，以下基于模型通用能力回答：这个问题当前没有可引用的企业知识库资料，因此只能给出通用建议。请结合实际业务背景判断；如果这是企业内部规范或项目专有问题，建议后续补充对应文档。\n\n引用来源：无\n置信度：较低\n是否需要补充文档：是";
        }
        if (prompt.contains("生成FAQ") || prompt.contains("FAQ")) {
            return "Q1：这篇文档主要解决什么问题？\nA1：它用于沉淀团队研发规范、操作步骤和常见问题。\n\nQ2：新人应该如何阅读？\nA2：先阅读摘要，再按步骤完成本地验证，最后结合 FAQ 复盘。";
        }
        if (prompt.contains("总结") || prompt.contains("摘要")) {
            return "文档摘要：本文档沉淀了团队研发知识的关键规范、操作步骤和注意事项。\n关键词：研发规范, 知识库, AI问答, 新人培训\n适用人群：新人 / 后端 / 前端 / 测试 / 运维\n阅读建议：先通读概览，再重点阅读操作步骤和异常处理部分。";
        }
        if (prompt.contains("学习计划") || prompt.contains("新人")) {
            return "第 1 天：了解团队项目结构和研发流程\n第 2 天：阅读开发规范与代码提交规范\n第 3 天：学习数据库与缓存使用规范\n第 4 天：本地启动项目并记录问题\n第 5 天：完成一个简单需求\n第 6 天：提交代码并参与 Code Review\n第 7 天：复盘常见问题并补充个人学习笔记";
        }
        return "直接回答：根据当前知识库检索结果，建议优先按照引用文档中的规范执行。\n\n操作步骤：\n1. 先确认问题所属知识空间和相关文档。\n2. 按引用片段中的配置、命名或流程要求检查。\n3. 如仍无法解决，将问题沉淀为未解决问题并补充文档。\n\n引用来源：见本次回答返回的 references。\n置信度：较高。\n是否需要补充文档：否。";
    }

    @Override
    public String complete(String prompt, List<ChatMemoryMessage> context) {
        String contextText = context == null || context.isEmpty() ? "" : "\nContext messages: " + context.size();
        return complete(prompt + contextText);
    }

    @Override
    public String complete(String prompt, List<ChatMemoryMessage> context, String model) {
        String modelText = model == null || model.isBlank() ? "" : "\nModel: " + model;
        return complete(prompt + modelText, context);
    }
}
