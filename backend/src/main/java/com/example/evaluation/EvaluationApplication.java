package com.example.evaluation;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class EvaluationApplication {

    public static void main(String[] args) {
        // Batik 光栅化在无头服务器（麒麟/LoongArch）上必须开启 headless
        System.setProperty("java.awt.headless", "true");
        SpringApplication.run(EvaluationApplication.class, args);
    }
}
