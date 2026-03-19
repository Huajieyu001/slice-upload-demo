package top.huajieyu001.domain;

import lombok.Data;

/**
 * 上传分片响应
 */
@Data
public class ChunkUploadVO {
    private Integer chunkIndex;
    private Integer uploadedChunks;
    private Integer totalChunks;
}