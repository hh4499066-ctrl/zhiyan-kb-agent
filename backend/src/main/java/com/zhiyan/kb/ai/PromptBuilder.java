package com.zhiyan.kb.ai;

import com.zhiyan.kb.dto.ChatMemoryMessage;
import com.zhiyan.kb.rag.RetrievalResult;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PromptBuilder {
    public String buildChatPrompt(String question, String rewrittenQuestion, List<String> longMemories, List<ChatMemoryMessage> shortMemory, List<RetrievalResult> retrievalResults) {
        StringBuilder sb = new StringBuilder();
        sb.append("系统角色：你是企业研发知识库 AI 智能协作体，必须优先依据企业内部知识片段回答，不允许编造。\n");
        sb.append("长期记忆：\n");
        longMemories.forEach(m -> sb.append("- ").append(m).append("\n"));
        sb.append("短期记忆：\n");
        shortMemory.forEach(m -> sb.append(m.getRole()).append("：").append(m.getContent()).append("\n"));
        sb.append("知识库检索结果：\n");
        if (retrievalResults.isEmpty()) {
            sb.append("没有检索到知识片段。\n");
        } else {
            retrievalResults.forEach(r -> sb.append("[").append(r.getDocumentTitle()).append("] ").append(r.getContent()).append("\n"));
        }
        sb.append("用户问题：").append(question).append("\n");
        sb.append("改写后问题：").append(rewrittenQuestion).append("\n");
        sb.append("输出格式：直接回答、操作步骤、引用来源、置信度、是否需要补充文档。\n");
        return sb.toString();
    }
}
