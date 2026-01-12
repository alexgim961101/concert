package com.example.concert.common.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

@Getter
@Setter
public class PageRequest {
    @Min(value = 0, message = "페이지 번호는 0 이상이어야 합니다")
    private int page = 0;

    @Min(value = 1, message = "페이지 크기는 1 이상이어야 합니다")
    @Max(value = 100, message = "페이지 크기는 100 이하여야 합니다")
    private int size = 10;

    private String sortBy = "id";
    private Sort.Direction direction = Sort.Direction.DESC;

    public Pageable toPageable() {
        return org.springframework.data.domain.PageRequest.of(page, size, Sort.by(direction, sortBy));
    }
}
