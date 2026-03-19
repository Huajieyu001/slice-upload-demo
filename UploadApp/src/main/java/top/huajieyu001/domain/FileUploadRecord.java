package top.huajieyu001.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;

/**
 * 
 * @TableName file_upload_record
 */
@TableName(value ="file_upload_record")
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
     * 原始文件名
     */
    private String fileName;

    /**
     * 文件大小
     */
    private Long fileSize;

    /**
     * 总分片数
     */
    private Integer totalChunks;

    /**
     * 已上传分片数
     */
    private Integer uploadedChunks;

    /**
     * 状态：0-上传中 1-已完成 2-已合并
     */
    private Integer status;

    /**
     * 最终文件路径
     */
    private String filePath;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}