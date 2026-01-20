package com.example.concert.domain.point.interfaces;

import com.example.concert.common.dto.ApiResponse;
import com.example.concert.domain.point.usecase.ChargePointUseCase;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/v1/points")
@RequiredArgsConstructor
public class PointController {

    private final ChargePointUseCase chargePointUseCase;

    @PatchMapping("/charge")
    public ResponseEntity<ApiResponse<ChargeResponse>> chargePoint(@Valid @RequestBody ChargeRequest request) {
        ChargePointUseCase.ChargeResult result = chargePointUseCase.execute(
                request.userId(),
                request.amount());

        return ResponseEntity.ok(ApiResponse.success(new ChargeResponse(
                result.userId(),
                result.currentBalance())));
    }

    // ===== DTOs =====
    public record ChargeRequest(
            @NotNull(message = "userId는 필수입니다") Long userId,

            @NotNull(message = "충전 금액은 필수입니다") @Min(value = 100, message = "최소 충전 금액은 100원입니다") BigDecimal amount) {
    }

    public record ChargeResponse(
            Long userId,
            BigDecimal currentBalance) {
    }
}
