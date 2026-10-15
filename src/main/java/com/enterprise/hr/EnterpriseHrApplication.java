package com.enterprise.hr;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 企业 HR 管理系统启动类
 *
 * @author Enterprise HR Team
 */
@SpringBootApplication
@MapperScan("com.enterprise.hr.**.mapper")
public class EnterpriseHrApplication {

    public static void main(String[] args) {
        SpringApplication.run(EnterpriseHrApplication.class, args);
    }
}
/* admin123 */