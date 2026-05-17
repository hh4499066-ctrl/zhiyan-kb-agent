package com.zhiyan.kb.vo;

import com.zhiyan.kb.entity.KbDocument;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class DocumentVO {
    private Long id;
    private Long spaceId;
    private String title;
    private String originalFilename;
    private String fileType;
    private Long fileSize;
    private String contentText;
    private String summary;
    private String keywords;
    private String parseStatus;
    private String vectorStatus;
    private String status;
    private Long uploaderId;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    public static DocumentVO from(KbDocument document) {
        DocumentVO vo = new DocumentVO();
        vo.setId(document.getId());
        vo.setSpaceId(document.getSpaceId());
        vo.setTitle(document.getTitle());
        vo.setOriginalFilename(document.getOriginalFilename());
        vo.setFileType(document.getFileType());
        vo.setFileSize(document.getFileSize());
        vo.setContentText(document.getContentText());
        vo.setSummary(document.getSummary());
        vo.setKeywords(document.getKeywords());
        vo.setParseStatus(document.getParseStatus());
        vo.setVectorStatus(document.getVectorStatus());
        vo.setStatus(document.getStatus());
        vo.setUploaderId(document.getUploaderId());
        vo.setCreateTime(document.getCreateTime());
        vo.setUpdateTime(document.getUpdateTime());
        return vo;
    }
}
