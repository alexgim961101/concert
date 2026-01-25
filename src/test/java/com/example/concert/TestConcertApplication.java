package com.example.concert;

import com.example.concert.config.AbstractIntegrationTest;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.context.TestConfiguration;

/**
 * 테스트용 애플리케이션 설정
 * 
 * 테스트 실행 시 AbstractIntegrationTest를 상속받아 Testcontainers가 자동 시작됩니다.
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestConcertApplication {

    public static void main(String[] args) {
        SpringApplication.from(ConcertApplication::main)
                .run(args);
    }
}
