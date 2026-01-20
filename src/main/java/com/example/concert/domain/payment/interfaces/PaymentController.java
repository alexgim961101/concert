package com.example.concert.domain.payment.interfaces;

import com.example.concert.common.dto.ApiResponse;
import com.example.concert.domain.payment.usecase.ProcessPaymentUseCase;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final ProcessPaymentUseCase processPaymentUseCase;

    @PostMapping
    public ResponseEntity<ApiResponse<PaymentResponse>> processPayment(
            @RequestHeader("Concert-Queue-Token") String token,
            @Valid @RequestBody PaymentRequest request) {

        ProcessPaymentUseCase.PaymentResult result = processPaymentUseCase.execute(
                token,
                request.userId(),
                request.reservationId());

        return ResponseEntity.ok(ApiResponse.success(new PaymentResponse(
                result.paymentId(),
                result.status(),
                result.amount(),
                result.paidAt())));
    }

    // ===== DTOs =====
    public record PaymentRequest(
            @NotNull(message = "userId는 필수입니다") Long userId,
            @NotNull(message = "reservationId는 필수입니다") Long reservationId) {
    }

    public record PaymentResponse(
            Long paymentId,
            String status,
            BigDecimal amount,
            LocalDateTime paidAt) {
    }
}
