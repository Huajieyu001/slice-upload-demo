package top.huajieyu001.service;

import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.List;

/**
 * @Author huajieyu
 * @Date 2026/3/19 20:07
 * @Version 1.0
 * @Description TODO
 */
public interface MinioService {

    void upload(String bucket, String objectName, MultipartFile file);

    void merge(String bucket, String objectName, List<String> chunkObjectNameList);

    long getFileSize(String bucket, String objectName);

    InputStream getInputStream(String bucket, String objectName);

    String calculateMd5(String bucket, String objectName);

    void delete(String bucket, String objectName);
}
