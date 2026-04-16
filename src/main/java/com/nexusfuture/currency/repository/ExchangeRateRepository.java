package com.nexusfuture.currency.repository;

import com.nexusfuture.currency.entity.ExchangeRate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ExchangeRateRepository extends JpaRepository<ExchangeRate, Long> {

    /**
     * 查找数据库中最新的一条汇率记录。
     *
     * @return 最新的汇率实体
     */
    Optional<ExchangeRate> findFirstByOrderByCreateTimeDesc();
}