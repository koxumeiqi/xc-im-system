package com.xcpowernode.im.service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {
        "com.xcpowernode.im.service", "com.xcpower.im"
})
public class IMApplication {

    public static void main(String[] args) {
        SpringApplication.run(IMApplication.class, args);
    }

}
