package com.igdm;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class IgDmWorkerApplication {

    public static void main(String[] args) {
        SpringApplication.run(IgDmWorkerApplication.class, args);
    }
}
