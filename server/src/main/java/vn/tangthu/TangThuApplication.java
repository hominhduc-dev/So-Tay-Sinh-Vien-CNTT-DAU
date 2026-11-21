package vn.tangthu;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class TangThuApplication {
    public static void main(String[] args) {
        SpringApplication.run(TangThuApplication.class, args);
    }
}
