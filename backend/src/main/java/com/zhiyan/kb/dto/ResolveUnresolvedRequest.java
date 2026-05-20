package com.zhiyan.kb.dto;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ResolveUnresolvedRequest {
    @Size(max = 500)
    private String resolveNote;

    @Positive
    private Long relatedDocumentId;
}
