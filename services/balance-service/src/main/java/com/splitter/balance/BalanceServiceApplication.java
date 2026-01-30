package com.splitter.balance;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Balance Service Application.
 * 
 * Handles balance calculations, debt tracking, and debt simplification.
 */
@SpringBootApplication
public class BalanceServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(BalanceServiceApplication.class, args);
    }
}
