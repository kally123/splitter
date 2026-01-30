package com.splitter.receipt;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class ReceiptServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ReceiptServiceApplication.class, args);
    }
}
