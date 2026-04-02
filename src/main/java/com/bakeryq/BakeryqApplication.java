package com.bakeryq;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class BakeryqApplication {

    public static void main(String[] args) {
        SpringApplication.run(BakeryqApplication.class, args);
    }
}
