package com.zhiyan.kb.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatMemoryMessage {
    private String role;
    private String content;
    private LocalDateTime createTime;
}
