package top.huajieyu001.service.impl;

import io.minio.*;
import io.minio.errors.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;
import org.springframework.web.multipart.MultipartFile;
import top.huajieyu001.service.MinioService;

import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

/**
 * @Author huajieyu
 * @Date 2026/3/19 18:32
 * @Version 1.0
 * @Description TODO
 */
@Service
public class MinioServiceImpl implements MinioService {

    @Autowired
    private MinioClient minioClient;

    @Override
    public void upload(String bucket, String objectName, MultipartFile file) {
        try {
            minioClient.putObject(
                    PutObjectArgs
                            .builder()
                            .bucket(bucket)
                            .object(objectName)
                            .stream(file.getInputStream(), file.getSize(), -1)
                            .build());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void merge(String bucket, String objectName, List<String> chunkObjectNameList) {
        if (chunkObjectNameList == null || chunkObjectNameList.isEmpty()) {
            throw new RuntimeException("分片文件名称为空");
        }
        List<ComposeSource> sourceList = new ArrayList<>();
        for (String chunkObjectName : chunkObjectNameList) {
            sourceList.add(ComposeSource
                    .builder()
                    .bucket(bucket)
                    .object(chunkObjectName)
                    .build());
        }

        // 合并
        try {
            minioClient.composeObject(
                    ComposeObjectArgs
                            .builder()
                            .bucket(bucket)
                            .object(objectName)
                            .sources(sourceList)
                            .build()
            );

            // 删除临时文件
//            for (String chunkObjectName : chunkObjectNameList) {
//                minioClient.removeObject(
//                        RemoveObjectArgs
//                                .builder()
//                                .bucket(bucket)
//                                .object(chunkObjectName)
//                                .build()
//                );
//            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public long getFileSize(String bucket, String objectName) {
        try {
            StatObjectResponse statObjectResponse = minioClient.statObject(
                    StatObjectArgs
                            .builder()
                            .bucket(bucket)
                            .object(objectName)
                            .build()
            );

            return statObjectResponse.size();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public InputStream getInputStream(String bucket, String objectName) {
        try {
            return minioClient.getObject(GetObjectArgs
                    .builder()
                    .bucket(bucket)
                    .object(objectName)
                    .build()
            );
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String calculateMd5(String bucket, String objectName) {
        try (InputStream inputStream = getInputStream(bucket, objectName);) {
//            MessageDigest md5 = MessageDigest.getInstance("MD5");
//            DigestInputStream dis = new DigestInputStream(inputStream, md5);
//
//            byte[] buffer = new byte[8192];
//
//            while (dis.read(buffer) != -1) {
//                // nothing
//            }

//            byte[] md5Bytes = dis.getMessageDigest().digest();
//
//            return DigestUtils.md5Hex(md5Bytes);

            return DigestUtils.md5DigestAsHex(inputStream);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void delete(String bucket, String objectName) {
        try {
            minioClient.removeObject(RemoveObjectArgs
                    .builder()
                    .bucket(bucket)
                    .object(objectName)
                    .build());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
