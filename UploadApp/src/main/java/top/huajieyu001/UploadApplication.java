package top.huajieyu001;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * @Author huajieyu
 * @Date 2026/3/19 1:16
 * @Version 1.0
 * @Description TODO
 */
@SpringBootApplication
@MapperScan("top.huajieyu001.mapper")
public class UploadApplication {

    public static void main(String[] args) {
        SpringApplication.run(UploadApplication.class, args);
    }
}
