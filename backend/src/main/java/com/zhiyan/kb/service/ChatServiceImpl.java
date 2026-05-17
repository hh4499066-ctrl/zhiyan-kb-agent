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
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

@Service
@Slf4j
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
    private final TransactionTemplate transactionTemplate;

    public ChatServiceImpl(ShortTermMemoryService shortTermMemoryService, QueryRewriteService queryRewriteService, LongTermMemoryService longTermMemoryService,
                           HybridRetrievalService retrievalService, PromptBuilder promptBuilder, LLMClient llmClient, AIResponseParser responseParser,
                           ChatRecordMapper chatRecordMapper, UnresolvedQuestionMapper unresolvedQuestionMapper, KbSpaceMapper spaceMapper,
                           ResourceAccessService accessService, TransactionTemplate transactionTemplate) {
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
        this.transactionTemplate = transactionTemplate;
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

        Long recordSpaceId = resolveRecordSpaceId(effectiveSpaceId, scopeSpaceId);
        ChatRecord record = persistChatRecord(userId, recordSpaceId, sessionId, request.getQuestion(), rewritten,
                answer, references, confidence, unresolved);
        rememberConversation(sessionId, userId, request.getQuestion(), answer);

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

    private ChatRecord persistChatRecord(Long userId, Long recordSpaceId, String sessionId, String question,
                                         String rewritten, String answer, List<ChatReferenceVO> references,
                                         double confidence, boolean unresolved) {
        return transactionTemplate.execute(status -> {
            ChatRecord record = new ChatRecord();
            record.setUserId(userId);
            record.setSpaceId(recordSpaceId);
            record.setSessionId(sessionId);
            record.setQuestion(question);
            record.setRewrittenQuestion(rewritten);
            record.setAnswer(answer);
            record.setReferencesJson(JSONUtil.toJsonStr(references));
            record.setConfidence(BigDecimal.valueOf(confidence));
            record.setUnresolved(unresolved);
            record.setFavorite(false);
            chatRecordMapper.insert(record);

            if (unresolved) {
                UnresolvedQuestion unresolvedQuestion = new UnresolvedQuestion();
                unresolvedQuestion.setUserId(userId);
                unresolvedQuestion.setSpaceId(recordSpaceId);
                unresolvedQuestion.setQuestion(question);
                unresolvedQuestion.setRewrittenQuestion(rewritten);
                unresolvedQuestion.setReason("RAG 检索结果为空或低于阈值");
                unresolvedQuestion.setStatus("PENDING");
                unresolvedQuestionMapper.insert(unresolvedQuestion);
            }
            incrementQaCount(recordSpaceId);
            return record;
        });
    }

    private void rememberConversation(String sessionId, Long userId, String question, String answer) {
        try {
            shortTermMemoryService.addMessage(sessionId, userId, "USER", question);
            shortTermMemoryService.addMessage(sessionId, userId, "ASSISTANT", answer);
        } catch (RuntimeException ex) {
            log.warn("Short-term memory write failed, sessionId={}, userId={}", sessionId, userId, ex);
        }
    }

    private void incrementQaCount(Long spaceId) {
        if (spaceId == null || spaceId <= 0) {
            return;
        }
        spaceMapper.update(null, new LambdaUpdateWrapper<KbSpace>()
                .eq(KbSpace::getId, spaceId)
                .setSql("qa_count = COALESCE(qa_count, 0) + 1"));
    }
}
