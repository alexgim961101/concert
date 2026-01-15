package com.example.concert.domain.concert.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface ConcertScheduleJpaRepository extends JpaRepository<ConcertScheduleJpaEntity, Long> {
    @Query("SELECT cs FROM ConcertScheduleJpaEntity cs WHERE cs.concert.id = :concertId")
    List<ConcertScheduleJpaEntity> findByConcertId(@Param("concertId") Long concertId);

    @Query("SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END FROM ConcertJpaEntity c WHERE c.id = :concertId")
    boolean existsConcertById(@Param("concertId") Long concertId);
}
