package com.example.concert.domain.payment.infrastructure;

import com.example.concert.domain.payment.entity.Payment;
import com.example.concert.domain.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class PaymentRepositoryImpl implements PaymentRepository {
    private final PaymentJpaRepository jpaRepository;

    @Override
    public Payment save(Payment payment) {
        PaymentJpaEntity entity = PaymentMapper.toEntity(payment);
        PaymentJpaEntity saved = jpaRepository.save(entity);
        return PaymentMapper.toDomain(saved);
    }
}
