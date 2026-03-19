package top.huajieyu001.cofig;

import io.minio.MinioClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import top.huajieyu001.properties.MinioProperties;

/**
 * @Author huajieyu
 * @Date 2026/3/19 18:24
 * @Version 1.0
 * @Description TODO
 */
@Configuration
public class MinioConfig {

    @Bean
    public MinioClient minioClient(MinioProperties minioProperties) {
        return MinioClient.builder()
                .endpoint(minioProperties.getEndpoint())
                .credentials(minioProperties.getAccessKey(), minioProperties.getSecretKey())
                .build();
    }
}
