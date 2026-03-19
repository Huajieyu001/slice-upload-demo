package top.huajieyu001.service;

import io.minio.MinioClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import top.huajieyu001.properties.MinioProperties;

/**
 * @Author huajieyu
 * @Date 2026/3/19 18:32
 * @Version 1.0
 * @Description TODO
 */
@Service
public class MinioService {

    @Autowired
    private MinioClient minioClient;

    @Autowired
    public MinioProperties minioProperties;
//
//    public String uploadFile(MultipartFile file) {
//        minioClient.uploadObject()
//    }

}
