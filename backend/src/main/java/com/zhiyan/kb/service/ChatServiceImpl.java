package com.zhiyan.kb.service;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import cn.hutool.core.util.IdUtil;
import cn.hutool.json.JSONUtil;
import com.zhiyan.kb.ai.AIResponseParser;
import com.zhiyan.kb.ai.LLMClient;
import com.zhiyan.kb.ai.PromptBuilder;
import com.zhiyan.kb.common.BusinessException;
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
import java.util.Set;

@Service
public class ChatServiceImpl implements ChatService {
    private static final double PRIMARY_MIN_SCORE = 0.18;
    private static final double FALLBACK_TRIGGER_SCORE = 0.35;
    private static final double FALLBACK_MIN_SCORE = 0.25;
    private static final int DEFAULT_TOP_K = 5;
    private static final int MAX_TOP_K = 20;
    private static final int MAX_QUESTION_LENGTH = 2000;
    private static final int MAX_ALL_SPACE_CANDIDATES = 20;
    private static final Set<String> ALLOWED_MODELS = Set.of("deepseek-v4-flash", "deepseek-v4-pro");

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
    private final ResourceAccessService accessService;

    public ChatServiceImpl(ShortTermMemoryService shortTermMemoryService, QueryRewriteService queryRewriteService, LongTermMemoryService longTermMemoryService,
                           HybridRetrievalService retrievalService, PromptBuilder promptBuilder, LLMClient llmClient, AIResponseParser responseParser,
                           ChatRecordMapper chatRecordMapper, UnresolvedQuestionMapper unresolvedQuestionMapper, KbSpaceMapper spaceMapper,
                           ResourceAccessService accessService) {
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
        this.accessService = accessService;
    }

    @Override
    public ChatAskResponse ask(ChatAskRequest request) {
        validateRequest(request);
        Long userId = UserContext.userId();
        String sessionId = request.getSessionId() == null || request.getSessionId().isBlank() ? IdUtil.fastSimpleUUID() : request.getSessionId();
        List<ChatMemoryMessage> context = Boolean.FALSE.equals(request.getUseMemory()) ? List.of() : shortTermMemoryService.getContext(sessionId, userId);
        String rewritten = queryRewriteService.rewrite(request.getQuestion(), context);
        List<String> longMemories = longTermMemoryService.recall(userId, rewritten, 3);
        Long scopeSpaceId = normalizeSpaceId(request.getSpaceId());
        if (scopeSpaceId != null) {
            accessService.requireSpaceAccess(scopeSpaceId);
        }
        Long effectiveSpaceId = scopeSpaceId;
        int topK = request.getTopK() == null ? DEFAULT_TOP_K : request.getTopK();
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
        String prompt = promptBuilder.buildChatPrompt(request.getQuestion(), rewritten, longMemories, retrievalResults);
        String answer = responseParser.compact(llmClient.complete(prompt, context, request.getModel()));
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
        incrementQaCount(effectiveSpaceId);

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
        List<Long> accessibleSpaceIds = accessService.accessibleNormalSpaceIds();
        if (accessibleSpaceIds.isEmpty()) {
            return List.of();
        }
        return spaceMapper.selectList(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<KbSpace>()
                        .in(KbSpace::getId, accessibleSpaceIds)
                        .eq(KbSpace::getStatus, "NORMAL")
                        .orderByDesc(KbSpace::getUpdateTime))
                .stream()
                .filter(space -> currentSpaceId == null || !space.getId().equals(currentSpaceId))
                .limit(MAX_ALL_SPACE_CANDIDATES)
                .flatMap(space -> retrievalService.search(space.getId(), question, topK, 0.5, FALLBACK_MIN_SCORE).stream())
                .sorted(Comparator.comparingDouble(RetrievalResult::getFinalScore).reversed())
                .limit(Math.max(1, topK))
                .toList();
    }

    private Long normalizeSpaceId(Long spaceId) {
        return spaceId == null || spaceId <= 0 ? null : spaceId;
    }

    private void validateRequest(ChatAskRequest request) {
        if (request.getQuestion() == null || request.getQuestion().isBlank()) {
            throw new BusinessException(400, "Question is required");
        }
        if (request.getQuestion().length() > MAX_QUESTION_LENGTH) {
            throw new BusinessException(400, "Question exceeds 2000 characters");
        }
        if (request.getTopK() != null && (request.getTopK() < 1 || request.getTopK() > MAX_TOP_K)) {
            throw new BusinessException(400, "topK must be between 1 and 20");
        }
        if (request.getModel() != null && !request.getModel().isBlank() && !ALLOWED_MODELS.contains(request.getModel())) {
            throw new BusinessException(400, "Unsupported chat model");
        }
    }

    private Long resolveRecordSpaceId(Long effectiveSpaceId, Long scopeSpaceId) {
        if (effectiveSpaceId != null) {
            return effectiveSpaceId;
        }
        if (scopeSpaceId != null) {
            return scopeSpaceId;
        }
        List<Long> accessibleSpaceIds = accessService.accessibleNormalSpaceIds();
        if (accessibleSpaceIds.isEmpty()) {
            return 0L;
        }
        KbSpace first = spaceMapper.selectList(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<KbSpace>()
                .in(KbSpace::getId, accessibleSpaceIds)
                .eq(KbSpace::getStatus, "NORMAL")).stream().findFirst().orElse(null);
        return first == null ? 0L : first.getId();
    }

    private void incrementQaCount(Long spaceId) {
        if (spaceId == null) {
            return;
        }
        spaceMapper.update(null, new LambdaUpdateWrapper<KbSpace>()
                .eq(KbSpace::getId, spaceId)
                .setSql("qa_count = COALESCE(qa_count, 0) + 1"));
    }
}
