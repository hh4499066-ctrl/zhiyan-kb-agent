package com.zhiyan.kb.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateDocumentRequest {
    @Size(max = 200)
    private String title;

    @Size(max = 2000)
    private String summary;

    @Size(max = 500)
    private String keywords;
}
