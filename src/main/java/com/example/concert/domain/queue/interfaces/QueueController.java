package com.example.concert.domain.queue.interfaces;

import com.example.concert.common.dto.ApiResponse;
import com.example.concert.domain.queue.usecase.IssueTokenUseCase;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/queue")
@RequiredArgsConstructor
public class QueueController {

    private final IssueTokenUseCase issueTokenUseCase;

    @PostMapping("/tokens")
    public ResponseEntity<ApiResponse<TokenResponse>> issueToken(
            @Valid @RequestBody TokenRequest request) {

        IssueTokenUseCase.IssueTokenResult result = issueTokenUseCase.execute(
                request.userId(),
                request.concertId());

        return ResponseEntity.ok(ApiResponse.success(new TokenResponse(
                result.token(),
                result.status(),
                result.rank(),
                result.estimatedWaitTime())));
    }

    // ===== DTOs =====
    public record TokenRequest(
            @NotNull(message = "userId는 필수입니다") Long userId,
            @NotNull(message = "concertId는 필수입니다") Long concertId) {
    }

    public record TokenResponse(
            String token,
            String status,
            long rank,
            long estimatedWaitTime) {
    }
}
