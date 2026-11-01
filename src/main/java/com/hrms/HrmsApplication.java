package com.hrms;

import com.hrms.config.ApplicationProperties;
import com.hrms.config.AuthProperties;
import com.hrms.config.EmployeeProperties;
import com.hrms.config.AttendanceProperties;
import com.hrms.email.BrevoProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties({ApplicationProperties.class, AuthProperties.class, EmployeeProperties.class,
        AttendanceProperties.class, BrevoProperties.class})
public class HrmsApplication {

    public static void main(String[] args) {
        SpringApplication.run(HrmsApplication.class, args);
    }
}
