package com.zhiyan.kb.vo;

import com.zhiyan.kb.entity.KbDocument;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class DocumentDetailVO extends DocumentListVO {
    private String contentText;

    public static DocumentDetailVO from(KbDocument document) {
        DocumentDetailVO vo = new DocumentDetailVO();
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
