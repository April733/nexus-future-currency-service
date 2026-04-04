package com.nexusfuture.currency.repository;

import com.nexusfuture.currency.entity.ExchangeRate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ExchangeRateRepository extends JpaRepository<ExchangeRate, Long> {

    /**
     * date 存 yyyy-MM-dd HH:mm:ss 时，用日历日前缀（yyyy-MM-dd）取当天最后一次拉取。
     */
    Optional<ExchangeRate> findFirstByDateStartingWithOrderByIdDesc(String datePrefix);
}