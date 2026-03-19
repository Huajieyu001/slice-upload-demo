package top.huajieyu001.domain;

import lombok.Data;

/**
 * 检查文件请求
 */
@Data
public class UploadCheckRequest {
    private String fileMd5;
    private String fileName;
}