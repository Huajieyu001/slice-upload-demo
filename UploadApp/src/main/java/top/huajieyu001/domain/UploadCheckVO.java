package top.huajieyu001.domain;

import lombok.Data;

import java.util.List;

/**
 * 检查文件响应
 */
@Data
public class UploadCheckVO {
    private Boolean shouldUpload;      // 是否需要上传
    private String fileUrl;            // 已存在时的文件路径
    private Integer uploadedChunks;    // 已上传分片数
    private Integer totalChunks;       // 总分片数
    private List<Integer> uploadedChunkIndexes;  // 已上传的分片索引
}