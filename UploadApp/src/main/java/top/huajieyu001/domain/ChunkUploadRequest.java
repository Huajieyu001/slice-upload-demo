package top.huajieyu001.domain;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

/**
 * 分片上传请求 DTO
 */
@Data
public class ChunkUploadRequest {

    // 文件 MD5
    private String fileMd5;

    // 原始文件名
    private String fileName;

    // 当前分片索引（从 0 开始）
    private Integer chunkIndex;

    // 总分片数
    private Integer totalChunks;

    // 分片大小
    private Long chunkSize;

    // 分片文件
    private MultipartFile chunkFile;
}