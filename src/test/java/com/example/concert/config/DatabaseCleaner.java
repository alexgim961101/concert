package com.example.concert.config;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Table;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 테스트 DB 클리너
 * 
 * @Transactional 없는 테스트(벤치마크, 동시성)에서 수동으로 DB를 정리할 때 사용합니다.
 *                FK 제약조건을 고려하여 TRUNCATE 순서를 지정합니다.
 */
@Component
public class DatabaseCleaner {

    @PersistenceContext
    private EntityManager entityManager;

    // TRUNCATE 순서 (FK 의존성 역순)
    private static final List<String> TABLE_NAMES = List.of(
            "payment",
            "reservation",
            "seat",
            "concert_schedule",
            "concert",
            "queue_token",
            "point");

    @Transactional
    public void cleanAll() {
        entityManager.flush();

        // FK 체크 비활성화
        entityManager.createNativeQuery("SET FOREIGN_KEY_CHECKS = 0").executeUpdate();

        for (String tableName : TABLE_NAMES) {
            entityManager.createNativeQuery("TRUNCATE TABLE " + tableName).executeUpdate();
        }

        // FK 체크 재활성화
        entityManager.createNativeQuery("SET FOREIGN_KEY_CHECKS = 1").executeUpdate();

        entityManager.clear();
    }

    /**
     * 특정 테이블만 클리어
     */
    @Transactional
    public void cleanTables(String... tableNames) {
        entityManager.flush();
        entityManager.createNativeQuery("SET FOREIGN_KEY_CHECKS = 0").executeUpdate();

        for (String tableName : tableNames) {
            entityManager.createNativeQuery("TRUNCATE TABLE " + tableName).executeUpdate();
        }

        entityManager.createNativeQuery("SET FOREIGN_KEY_CHECKS = 1").executeUpdate();
        entityManager.clear();
    }
}
