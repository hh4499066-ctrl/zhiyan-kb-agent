package com.zhiyan.kb.service;

import com.zhiyan.kb.entity.ChatRecord;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class DashboardService {
    public String metricName() {
        return "overview";
    }

    public List<Map<String, Object>> topQuestions(List<ChatRecord> records) {
        return records.stream()
                .filter(record -> record.getQuestion() != null && !record.getQuestion().isBlank())
                .collect(Collectors.groupingBy(record -> normalizeQuestion(record.getQuestion()), LinkedHashMap::new, Collectors.toList()))
                .values()
                .stream()
                .filter(group -> !group.isEmpty() && !normalizeQuestion(group.get(0).getQuestion()).isBlank())
                .map(this::toTopQuestion)
                .sorted(Comparator
                        .comparingLong(TopQuestion::count).reversed()
                        .thenComparing(TopQuestion::latestTime, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(10)
                .map(item -> {
                    Map<String, Object> result = new LinkedHashMap<>();
                    result.put("question", item.question());
                    result.put("count", item.count());
                    return result;
                })
                .toList();
    }

    private TopQuestion toTopQuestion(List<ChatRecord> group) {
        ChatRecord latest = group.stream()
                .max(Comparator.comparing(ChatRecord::getCreateTime, Comparator.nullsLast(Comparator.naturalOrder())))
                .orElse(group.get(0));
        return new TopQuestion(latest.getQuestion(), group.size(), latest.getCreateTime());
    }

    private String normalizeQuestion(String question) {
        return Normalizer.normalize(question, Normalizer.Form.NFKC)
                .trim()
                .toLowerCase()
                .replaceAll("[\\s\\p{Punct}\\uFF0C\\u3002\\uFF01\\uFF1F\\uFF1B\\uFF1A\\u3001\\u201C\\u201D\\u2018\\u2019\\uFF08\\uFF09\\u3010\\u3011\\u300A\\u300B\\u2026\\u2014]+", "");
    }

    private record TopQuestion(String question, long count, LocalDateTime latestTime) {
    }
}
