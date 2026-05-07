package com.zhiyan.kb.service;

import cn.hutool.core.util.IdUtil;
import cn.hutool.json.JSONUtil;
import com.zhiyan.kb.ai.AIResponseParser;
import com.zhiyan.kb.ai.LLMClient;
import com.zhiyan.kb.ai.PromptBuilder;
import com.zhiyan.kb.common.UserContext;
import com.zhiyan.kb.dto.ChatAskRequest;
import com.zhiyan.kb.dto.ChatMemoryMessage;
import com.zhiyan.kb.entity.ChatRecord;
import com.zhiyan.kb.entity.KbSpace;
import com.zhiyan.kb.entity.UnresolvedQuestion;
import com.zhiyan.kb.mapper.ChatRecordMapper;
import com.zhiyan.kb.mapper.KbSpaceMapper;
import com.zhiyan.kb.mapper.UnresolvedQuestionMapper;
import com.zhiyan.kb.rag.HybridRetrievalService;
import com.zhiyan.kb.rag.RetrievalResult;
import com.zhiyan.kb.vo.ChatAskResponse;
import com.zhiyan.kb.vo.ChatReferenceVO;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

@Service
public class ChatServiceImpl implements ChatService {
    private static final double PRIMARY_MIN_SCORE = 0.18;
    private static final double FALLBACK_TRIGGER_SCORE = 0.35;
    private static final double FALLBACK_MIN_SCORE = 0.25;

    private final ShortTermMemoryService shortTermMemoryService;
    private final QueryRewriteService queryRewriteService;
    private final LongTermMemoryService longTermMemoryService;
    private final HybridRetrievalService retrievalService;
    private final PromptBuilder promptBuilder;
    private final LLMClient llmClient;
    private final AIResponseParser responseParser;
    private final ChatRecordMapper chatRecordMapper;
    private final UnresolvedQuestionMapper unresolvedQuestionMapper;
    private final KbSpaceMapper spaceMapper;

    public ChatServiceImpl(ShortTermMemoryService shortTermMemoryService, QueryRewriteService queryRewriteService, LongTermMemoryService longTermMemoryService,
                           HybridRetrievalService retrievalService, PromptBuilder promptBuilder, LLMClient llmClient, AIResponseParser responseParser,
                           ChatRecordMapper chatRecordMapper, UnresolvedQuestionMapper unresolvedQuestionMapper, KbSpaceMapper spaceMapper) {
        this.shortTermMemoryService = shortTermMemoryService;
        this.queryRewriteService = queryRewriteService;
        this.longTermMemoryService = longTermMemoryService;
        this.retrievalService = retrievalService;
        this.promptBuilder = promptBuilder;
        this.llmClient = llmClient;
        this.responseParser = responseParser;
        this.chatRecordMapper = chatRecordMapper;
        this.unresolvedQuestionMapper = unresolvedQuestionMapper;
        this.spaceMapper = spaceMapper;
    }

    @Override
    public ChatAskResponse ask(ChatAskRequest request) {
        Long userId = UserContext.userId();
        String sessionId = request.getSessionId() == null || request.getSessionId().isBlank() ? IdUtil.fastSimpleUUID() : request.getSessionId();
        List<ChatMemoryMessage> context = Boolean.FALSE.equals(request.getUseMemory()) ? List.of() : shortTermMemoryService.getContext(sessionId, userId);
        String rewritten = queryRewriteService.rewrite(request.getQuestion(), context);
        List<String> longMemories = longTermMemoryService.recall(userId, rewritten, 3);
        Long scopeSpaceId = normalizeSpaceId(request.getSpaceId());
        Long effectiveSpaceId = scopeSpaceId;
        int topK = request.getTopK() == null ? 5 : request.getTopK();
        List<RetrievalResult> retrievalResults = scopeSpaceId == null
                ? searchAllSpaces(null, rewritten, topK)
                : retrievalService.search(scopeSpaceId, rewritten, topK, 0.5, PRIMARY_MIN_SCORE);
        if (scopeSpaceId != null && shouldFallback(retrievalResults)) {
            List<RetrievalResult> fallbackResults = searchAllSpaces(scopeSpaceId, rewritten, topK);
            if (!fallbackResults.isEmpty()) {
                retrievalResults = fallbackResults;
            }
        }
        if (!retrievalResults.isEmpty()) {
            effectiveSpaceId = retrievalResults.get(0).getSpaceId();
        }
        boolean unresolved = retrievalResults.isEmpty();
        String prompt = promptBuilder.buildChatPrompt(request.getQuestion(), rewritten, longMemories, context, retrievalResults);
        String answer = responseParser.compact(llmClient.complete(prompt));
        double confidence = unresolved ? 0.25 : Math.min(0.95, retrievalResults.get(0).getFinalScore() + 0.35);
        List<ChatReferenceVO> references = retrievalResults.stream()
                .map(r -> new ChatReferenceVO(r.getDocumentId(), r.getDocumentTitle(), r.getChunkId(), r.getContent(), r.getFinalScore()))
                .toList();

        ChatRecord record = new ChatRecord();
        record.setUserId(userId);
        record.setSpaceId(resolveRecordSpaceId(effectiveSpaceId, scopeSpaceId));
        record.setSessionId(sessionId);
        record.setQuestion(request.getQuestion());
        record.setRewrittenQuestion(rewritten);
        record.setAnswer(answer);
        record.setReferencesJson(JSONUtil.toJsonStr(references));
        record.setConfidence(BigDecimal.valueOf(confidence));
        record.setUnresolved(unresolved);
        record.setFavorite(false);
        chatRecordMapper.insert(record);

        if (unresolved) {
            UnresolvedQuestion question = new UnresolvedQuestion();
            question.setUserId(userId);
            question.setSpaceId(resolveRecordSpaceId(effectiveSpaceId, scopeSpaceId));
            question.setQuestion(request.getQuestion());
            question.setRewrittenQuestion(rewritten);
            question.setReason("RAG 检索结果为空或低于阈值");
            question.setStatus("PENDING");
            unresolvedQuestionMapper.insert(question);
        }
        shortTermMemoryService.addMessage(sessionId, userId, "USER", request.getQuestion());
        shortTermMemoryService.addMessage(sessionId, userId, "ASSISTANT", answer);
        if (effectiveSpaceId != null && spaceMapper.selectById(effectiveSpaceId) != null) {
            spaceMapper.update(null, new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<com.zhiyan.kb.entity.KbSpace>()
                    .eq(com.zhiyan.kb.entity.KbSpace::getId, effectiveSpaceId)
                    .setSql("qa_count = qa_count + 1"));
        }

        ChatAskResponse response = new ChatAskResponse();
        response.setAnswer(answer);
        response.setRewrittenQuestion(rewritten);
        response.setReferences(references);
        response.setConfidence(confidence);
        response.setSessionId(sessionId);
        response.setSpaceId(effectiveSpaceId);
        response.setRecordId(record.getId());
        response.setUnresolved(unresolved);
        return response;
    }

    private boolean shouldFallback(List<RetrievalResult> results) {
        return results.isEmpty() || results.get(0).getFinalScore() < FALLBACK_TRIGGER_SCORE;
    }

    private List<RetrievalResult> searchAllSpaces(Long currentSpaceId, String question, int topK) {
        return spaceMapper.selectList(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<KbSpace>()
                        .eq(KbSpace::getStatus, "NORMAL"))
                .stream()
                .filter(space -> currentSpaceId == null || !space.getId().equals(currentSpaceId))
                .flatMap(space -> retrievalService.search(space.getId(), question, topK, 0.5, FALLBACK_MIN_SCORE).stream())
                .sorted(Comparator.comparingDouble(RetrievalResult::getFinalScore).reversed())
                .limit(Math.max(1, topK))
                .toList();
    }

    private Long normalizeSpaceId(Long spaceId) {
        return spaceId == null || spaceId <= 0 ? null : spaceId;
    }

    private Long resolveRecordSpaceId(Long effectiveSpaceId, Long scopeSpaceId) {
        if (effectiveSpaceId != null) {
            return effectiveSpaceId;
        }
        if (scopeSpaceId != null) {
            return scopeSpaceId;
        }
        KbSpace first = spaceMapper.selectList(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<KbSpace>()
                .eq(KbSpace::getStatus, "NORMAL")
                .last("limit 1")).stream().findFirst().orElse(null);
        return first == null ? 0L : first.getId();
    }
}
