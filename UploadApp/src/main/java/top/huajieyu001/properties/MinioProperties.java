package top.huajieyu001.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * @Author huajieyu
 * @Date 2026/3/19 18:28
 * @Version 1.0
 * @Description TODO
 */
@ConfigurationProperties(prefix = "minio")
@Configuration
@Data
public class MinioProperties {
    private String endpoint;
    private String accessKey;
    private String secretKey;
    private String bucket;
}
