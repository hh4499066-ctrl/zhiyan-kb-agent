package com.zhiyan.kb.ai;

import com.zhiyan.kb.rag.RetrievalResult;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PromptBuilder {
    public String buildChatPrompt(String question, String rewrittenQuestion, List<String> longMemories,
                                  List<RetrievalResult> retrievalResults) {
        StringBuilder sb = new StringBuilder();
        sb.append("记忆归属规则：长期记忆只描述当前登录用户。如果记忆中出现“我”“我的”等表述，必须理解为用户的身份、偏好或项目背景，不是 AI 助手自己的身份。\n");
        sb.append("系统角色：你是企业研发知识库 AI 智能协作体。优先依据企业内部知识片段回答；如果没有检索到相关知识片段，可以使用模型通用能力回答，但必须明确说明没有可引用的知识库来源，不能伪造引用。\n");
        appendLongMemories(sb, longMemories);
        appendRetrievalResults(sb, retrievalResults);
        sb.append("用户问题：").append(question).append("\n");
        sb.append("改写后问题：").append(rewrittenQuestion).append("\n");
        sb.append("输出要求：直接回答问题；如有知识库来源，列出引用来源和置信度；如无知识库来源，说明“未检索到相关知识库来源，以下基于模型通用能力回答”。\n");
        return sb.toString();
    }

    private void appendLongMemories(StringBuilder sb, List<String> longMemories) {
        sb.append("长期记忆：\n");
        if (longMemories == null || longMemories.isEmpty()) {
            sb.append("无相关长期记忆。\n");
            return;
        }
        longMemories.forEach(memory -> sb.append("- ").append(memory).append("\n"));
    }

    private void appendRetrievalResults(StringBuilder sb, List<RetrievalResult> retrievalResults) {
        sb.append("知识库检索结果：\n");
        if (retrievalResults == null || retrievalResults.isEmpty()) {
            sb.append("未检索到相关知识库片段。请基于模型通用能力回答，并明确说明没有可引用的知识库来源。\n");
            return;
        }
        retrievalResults.forEach(result -> sb.append("[")
                .append(result.getDocumentTitle())
                .append("] ")
                .append(result.getContent())
                .append("\n"));
    }
}
