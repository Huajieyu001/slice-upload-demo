package top.huajieyu001.domain;

import lombok.Data;

/**
 * 合并文件响应
 */
@Data
public class MergeVO {
    private String fileUrl;
    private String fileName;
    private Long fileSize;
}