package top.huajieyu001.service;

import com.baomidou.mybatisplus.extension.service.IService;
import top.huajieyu001.domain.*;

/**
 * @author xanadu
 * @description 针对表【file_upload_record】的数据库操作Service
 * @createDate 2026-03-19 14:09:14
 */
public interface FileUploadRecordService extends IService<FileUploadRecord> {

    UploadCheckVO check(UploadCheckRequest checkReqDto);

    ChunkUploadVO chunkUpload(ChunkUploadRequest chunkUploadRequest);

    UploadProgressVO progress(String fileMd5);

    void sync(String fileMd5);
}
