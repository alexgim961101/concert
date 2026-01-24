package com.example.concert.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    // 공통 에러
    INTERNAL_SERVER_ERROR("E0001", "내부 서버 오류가 발생했습니다"),
    INVALID_INPUT_VALUE("E0002", "잘못된 입력 값입니다"),
    METHOD_NOT_ALLOWED("E0003", "허용되지 않은 HTTP 메서드입니다"),

    // 인증/인가 에러
    UNAUTHORIZED("E1001", "인증이 필요합니다"),
    FORBIDDEN("E1002", "접근 권한이 없습니다"),

    // 리소스 에러
    RESOURCE_NOT_FOUND("E2001", "요청한 리소스를 찾을 수 없습니다"),
    RESOURCE_ALREADY_EXISTS("E2002", "이미 존재하는 리소스입니다"),

    // 비즈니스 로직 에러
    BUSINESS_LOGIC_ERROR("E3001", "비즈니스 로직 오류가 발생했습니다"),

    // 데이터베이스 에러
    DATA_ACCESS_ERROR("E4001", "데이터베이스 오류가 발생했습니다"),

    // 동시성 에러
    CONCURRENCY_CONFLICT("E4002", "다른 요청과 충돌이 발생했습니다. 다시 시도해 주세요."),

    // 외부 서비스 에러
    EXTERNAL_SERVICE_ERROR("E5001", "외부 서비스 오류가 발생했습니다");

    private final String code;
    private final String message;
}
