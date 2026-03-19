package top.huajieyu001.domain;

import lombok.Data;

/**
 * 合并文件请求
 */
@Data
public class MergeRequest {
    private String fileMd5;
    private String fileName;
}