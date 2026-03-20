package top.huajieyu001.domain;

import lombok.Data;

/**
 * 查询进度响应
 */
@Data
public class UploadProgressVO {
    private Integer progress;
    private Integer status;
    private Integer uploadedChunks;
    private Integer totalChunks;
}