package top.huajieyu001.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.huajieyu001.constant.PathConstants;
import top.huajieyu001.constant.RedisConstants;
import top.huajieyu001.domain.*;
import top.huajieyu001.mapper.FileUploadRecordMapper;
import top.huajieyu001.properties.MinioProperties;
import top.huajieyu001.service.FileUploadRecordService;
import top.huajieyu001.service.MinioService;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;

/**
 * @author xanadu
 * @description 针对表【file_upload_record】的数据库操作Service实现
 * @createDate 2026-03-19 14:09:14
 */
@Service
@Slf4j
public class FileUploadRecordServiceImpl extends ServiceImpl<FileUploadRecordMapper, FileUploadRecord>
        implements FileUploadRecordService {

    @Resource
    private RedisTemplate redisTemplate;

    @Resource
    private MinioProperties minioProperties;

    @Resource
    private MinioService minioService;

    @Resource
    private ThreadPoolExecutor fileProcessThreadPool;

    @Override
    public UploadCheckVO check(UploadCheckRequest checkRequest) {
        UploadCheckVO vo = new UploadCheckVO();

        Map entries = redisTemplate.opsForHash().entries(RedisConstants.FILE_PROCESS_UPLOAD_INFO_PREFIX + checkRequest.getFileMd5());
        if (entries != null && entries.size() > 0) {
            String objectName = (String) entries.get("objectName");
            if (objectName != null) {
                // md5存在，并且状态为已经合并可以秒传
                vo.setFileUrl(objectName);
                vo.setShouldUpload(false);
                return vo;
            }
            // 如果存在entries但是objectName为null。说明此时是已经开始上传但还未上传成功
            // 获取已上传的分片索引返回
            Set<Integer> members = redisTemplate.opsForSet().members(RedisConstants.FILE_PROCESS_UPLOADED_CHUNK_SET_PREFIX + checkRequest.getFileMd5());
            vo.setUploadedChunkIndexes(new ArrayList<>(members));
            // 如果已经上传成功但只是还未合并，则触发合并操作
            if (members.size() == checkRequest.getTotalChunks()) {
                sync(checkRequest.getFileMd5());
            }
        } else {
            // 不存在对应md5的hash信息，则新建一份记录总分片数和文件名称信息（为了获取文件类型）
            Map<String, Object> map = new HashMap<>();
            map.put("totalChunks", checkRequest.getTotalChunks());
            map.put("fileName", checkRequest.getFileName());
            redisTemplate.opsForHash().putAll(RedisConstants.FILE_PROCESS_UPLOAD_INFO_PREFIX + checkRequest.getFileMd5(), map);
        }
        vo.setShouldUpload(true);
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ChunkUploadVO chunkUpload(ChunkUploadRequest chunkUploadRequest) {
        String key = RedisConstants.FILE_PROCESS_UPLOADED_CHUNK_SET_PREFIX + chunkUploadRequest.getFileMd5();

        Set members = redisTemplate.opsForSet().members(key);

        // 如果md5对应分片已经上传过，则终止后续处理
        if (members != null && members.contains(chunkUploadRequest.getChunkIndex())) {
            return null;
        }

        // 上传到Minio中
        minioService.upload(minioProperties.getBucket(),
                PathConstants.TEMP_CHUNK + "/" + chunkUploadRequest.getFileMd5() + "/" + chunkUploadRequest.getChunkIndex(),
                chunkUploadRequest.getChunkFile()
        );

        // 上传成功把分片索引记录到redis中
        redisTemplate.opsForSet().add(key, chunkUploadRequest.getChunkIndex());

        int uploadedChunks = redisTemplate.opsForSet().members(key).size();

        ChunkUploadVO vo = new ChunkUploadVO();
        vo.setUploadedChunks(uploadedChunks);
        vo.setChunkIndex(chunkUploadRequest.getChunkIndex());
        vo.setTotalChunks(chunkUploadRequest.getTotalChunks());

        log.info("uploadedChunks is {}", uploadedChunks);
        if (uploadedChunks == chunkUploadRequest.getTotalChunks()) {
            // 触发一次合并操作
            sync(chunkUploadRequest.getFileMd5());
        }
        return vo;
    }

    @Override
    public UploadProgressVO progress(String fileMd5) {
        UploadProgressVO vo = new UploadProgressVO();
        Set<Integer> members = redisTemplate.opsForSet().members(RedisConstants.FILE_PROCESS_UPLOADED_CHUNK_SET_PREFIX + fileMd5);
        if (members == null || members.size() == 0) {
            vo.setProgress(0);
            vo.setStatus(0);
            vo.setUploadedChunks(0);
            vo.setTotalChunks(0);
        } else {
            Map entries = redisTemplate.opsForHash().entries(RedisConstants.FILE_PROCESS_UPLOAD_INFO_PREFIX + fileMd5);
            Integer totalChunks = (Integer) entries.get("totalChunks");
            vo.setProgress(members.size() * 100 / totalChunks);
            vo.setTotalChunks(totalChunks);
            vo.setUploadedChunks(members.size());
            if (members.size() < totalChunks) {
                vo.setStatus(1);
            } else {
                vo.setStatus(2);
                sync(fileMd5);
            }
        }
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void sync(String fileMd5) {
        fileProcessThreadPool.execute(() -> {
            FileUploadRecord existsRecord = lambdaQuery().eq(FileUploadRecord::getFileMd5, fileMd5).one();
            if (existsRecord != null) {
                log.info("数据已经合并和落库，跳过操作，md5={}", fileMd5);
                return;
            }
            Set members = redisTemplate.opsForSet().members(RedisConstants.FILE_PROCESS_UPLOADED_CHUNK_SET_PREFIX + fileMd5);
            Map entries = redisTemplate.opsForHash().entries(RedisConstants.FILE_PROCESS_UPLOAD_INFO_PREFIX + fileMd5);
            if (entries == null) {
                throw new RuntimeException("entries is null, md5 = " + fileMd5);
            }
            Integer totalChunks = (Integer) entries.get("totalChunks");
            String fileName = (String) entries.get("fileName");

            if (members == null || members.size() != totalChunks) {
                return;
            }

            List<Integer> list = new ArrayList(members);
            Collections.sort(list);

            List<String> nameList = list.stream().map(x -> PathConstants.TEMP_CHUNK + "/" + fileMd5 + "/" + x).collect(Collectors.toList());

            String objectName = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd")) + "/" + UUID.randomUUID() + fileName;
            minioService.merge(minioProperties.getBucket(), objectName, nameList);
            String calculatedMd5 = minioService.calculateMd5(minioProperties.getBucket(), objectName);
            if (!calculatedMd5.equals(fileMd5)) {
                clearAllData(fileMd5, minioProperties.getBucket(), objectName);
                throw new RuntimeException("Md5 does not match, expected: " + fileMd5 + ", actual: " + calculatedMd5);
            }
            log.info("md5 matched, expected: {}, actual: {}", fileMd5, calculatedMd5);
            redisTemplate.opsForHash().put(RedisConstants.FILE_PROCESS_UPLOAD_INFO_PREFIX + fileMd5, "objectName", objectName);

            FileUploadRecord record = new FileUploadRecord();
            record.setFileMd5(fileMd5);
            record.setBucket(minioProperties.getBucket());
            record.setObjectName(objectName);
            record.setFileSize(minioService.getFileSize(minioProperties.getBucket(), objectName));
            record.setCreateTime(LocalDateTime.now());
            record.setUpdateTime(LocalDateTime.now());
            this.save(record);
        });
    }

    private void clearAllData(String md5, String bucket, String objectName) {
        redisTemplate.delete(RedisConstants.FILE_PROCESS_UPLOAD_INFO_PREFIX + md5);
        redisTemplate.delete(RedisConstants.FILE_PROCESS_UPLOADED_CHUNK_SET_PREFIX + md5);
        minioService.delete(bucket, objectName);
    }
}