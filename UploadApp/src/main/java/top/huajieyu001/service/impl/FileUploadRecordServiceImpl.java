package top.huajieyu001.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.apache.tomcat.util.http.fileupload.FileUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;
import top.huajieyu001.domain.*;
import top.huajieyu001.mapper.FileUploadRecordMapper;
import top.huajieyu001.service.FileUploadRecordService;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
* @author xanadu
* @description 针对表【file_upload_record】的数据库操作Service实现
* @createDate 2026-03-19 14:09:14
*/
@Service
@Slf4j
public class FileUploadRecordServiceImpl extends ServiceImpl<FileUploadRecordMapper, FileUploadRecord>
    implements FileUploadRecordService {

    @Value("${mine.upload.temp-dir}")
    private String tempDir;

    @Value("${mine.upload.target-dir}")
    private String targetDir;

    @Override
    public UploadCheckVO check(UploadCheckRequest checkRequest) {
        FileUploadRecord record = lambdaQuery().eq(FileUploadRecord::getFileMd5, checkRequest.getFileMd5()).one();
        UploadCheckVO vo = new UploadCheckVO();

        // 秒传
        if (record != null && record.getStatus() == 2) {
            vo.setFileUrl(record.getFilePath());
            vo.setShouldUpload(false);
            return vo;
        }

        vo.setShouldUpload(true);

        // 状态为0说明未上传完成所有分片
        if(record != null && record.getStatus() == 0) {
            vo.setTotalChunks(record.getTotalChunks());
            vo.setUploadedChunks(record.getUploadedChunks());
            vo.setUploadedChunkIndexes(getUploadedChunkIndexes(checkRequest.getFileMd5()));
        }
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ChunkUploadVO chunkUpload(ChunkUploadRequest chunkUploadRequest) {
        ChunkUploadVO vo = new ChunkUploadVO();
        String chunkDir = getChunkDir(chunkUploadRequest.getFileMd5());
        File dir = new File(chunkDir);

        if(!dir.exists()) {
            dir.mkdirs();
        }

        String chunkPath = chunkDir + File.separator + chunkUploadRequest.getChunkIndex();
        File chunkFile = new File(chunkPath);

        try{
            chunkUploadRequest.getChunkFile().transferTo(chunkFile);
            log.info("分片上传成功，md5为{}，分片为{}", chunkUploadRequest.getFileMd5(), chunkUploadRequest.getChunkIndex());
        } catch (Exception e) {
            log.error("分片保存失败", e);
            throw new RuntimeException(e);
        }

        FileUploadRecord record = lambdaQuery().eq(FileUploadRecord::getFileMd5, chunkUploadRequest.getFileMd5()).one();
        if(record == null) {
            record = new FileUploadRecord();
            record.setFileMd5(chunkUploadRequest.getFileMd5());
            record.setFileName(chunkUploadRequest.getFileName());
            record.setTotalChunks(chunkUploadRequest.getTotalChunks());
            record.setUploadedChunks(1);
            record.setStatus(0);
            record.setCreateTime(LocalDateTime.now());
            this.save(record);
        } else {
            List<Integer> uploadedIndexes = getUploadedChunkIndexes(chunkUploadRequest.getFileMd5());
            if(!uploadedIndexes.contains(chunkUploadRequest.getChunkIndex())) {
                record.setUploadedChunks(record.getUploadedChunks() + 1);
                record.setUpdateTime(LocalDateTime.now());
                updateById(record);
            }
        }
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MergeVO merge(MergeRequest mergeRequest) {
        MergeVO vo = new MergeVO();
        FileUploadRecord record = lambdaQuery().eq(FileUploadRecord::getFileMd5, mergeRequest.getFileMd5()).one();
        if (record == null) {
            throw new RuntimeException("上传记录不存在");
        }

        if (getUploadedChunkIndexes(mergeRequest.getFileMd5()).size() < record.getTotalChunks()) {
            throw new RuntimeException("分片未上传完成");
        }

        // 1. 创建最终文件目录
        String finalDir = targetDir + "/" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        File dir = new File(finalDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        String finalFilePath = finalDir + File.separator + UUID.randomUUID().toString() + "__" + record.getFileName();
        File finalFile = new File(finalFilePath);

        String chunkDir = getChunkDir(mergeRequest.getFileMd5());
        try(FileOutputStream fos = new FileOutputStream(finalFile)) {
            for (int i = 0; i < record.getTotalChunks(); i++) {
                File chunkFile = new File(chunkDir + File.separator + i);
                if(chunkFile == null || !chunkFile.exists()) {
                    throw new RuntimeException("分片不存在，对应索引为：" + i);
                }
                try(FileInputStream fis = new FileInputStream(chunkFile)) {
                    byte[] buffer = new byte[8192];
                    int len;
                    while ((len = fis.read(buffer)) > 0) {
                        fos.write(buffer, 0, len);
                    }
                }
            }
            log.info("文件合并成功{}", finalFilePath);
        } catch (IOException e){
            log.error("文件合并失败", e);
            throw new RuntimeException(e);
        }

        // 校验文件md5
        String finalMd5 = calMd5(finalFile);
        if(!finalMd5.equals(mergeRequest.getFileMd5())) {
            log.error("md5校验失败，期望是{}，实际是{}", mergeRequest.getFileMd5(), finalMd5);
            finalFile.delete();
            throw new RuntimeException("文件完整性校验失败");
        }

        record.setStatus(2);
        record.setFilePath(finalFilePath);
        record.setFileSize(finalFile.length());
        record.setUpdateTime(LocalDateTime.now());
        updateById(record);

        // 清理临时文件
        cleanupChunkFiles(mergeRequest.getFileMd5());

        vo.setFileUrl(finalFilePath);
        vo.setFileName(record.getFileName());
        vo.setFileSize(record.getFileSize());
        return vo;
    }

    @Override
    public UploadProgressVO progress(String fileMd5) {
        UploadProgressVO vo = new UploadProgressVO();
        FileUploadRecord record = lambdaQuery().eq(FileUploadRecord::getFileMd5, fileMd5).one();

        if(record == null) {
            vo.setProgress(0);
            vo.setStatus(0);
            return vo;
        }

        vo.setProgress(record.getUploadedChunks() * 100 / record.getTotalChunks());
        vo.setStatus(record.getStatus());
        vo.setTotalChunks(record.getTotalChunks());
        vo.setUploadedChunks(record.getUploadedChunks());
        return vo;
    }

    private List<Integer> getUploadedChunkIndexes(String fileMd5) {
        List<Integer> list = new ArrayList<>();
        String chunkDir = getChunkDir(fileMd5);
        File dir = new File(chunkDir);
        if (dir.exists()) {
            File[] files = dir.listFiles();
            for (File file : files) {
                list.add(Integer.parseInt(file.getName()));
            }
        }
        return list;
    }

    private String getChunkDir(String fileMd5) {
        return tempDir + "/chunks/" + fileMd5;
    }

    private void cleanupChunkFiles(String fileMd5) {
        String chunkDir = getChunkDir(fileMd5);
        File dir = new File(chunkDir);
        if(dir.exists()){
            CompletableFuture.runAsync(() -> {
                try {
                    Thread.sleep(30000);
                    FileUtils.deleteDirectory(dir);
                    log.info("临时文件已删除{}", fileMd5);
                } catch (InterruptedException | IOException e) {
                    log.error("临时文件删除失败:", e);
                }
            });
        }
    }

    private String calMd5(File file) {
        try(FileInputStream fis = new FileInputStream(file)){
            return DigestUtils.md5DigestAsHex(fis);
        } catch (IOException e){
            log.error("md5计算失败:", e);
            throw new RuntimeException(e);
        }
    }
}




