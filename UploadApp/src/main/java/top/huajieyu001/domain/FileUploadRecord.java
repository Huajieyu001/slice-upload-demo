package top.huajieyu001.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * @TableName file_upload_record
 */
@TableName(value = "file_upload_record")
@Data
public class FileUploadRecord {
    /**
     * 主键
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 文件 MD5
     */
    private String fileMd5;

    /**
     * 文件存储桶名
     */
    private String bucket;

    /**
     * 原始文件名
     */
    private String objectName;


    /**
     * 文件大小
     */
    private Long fileSize;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}