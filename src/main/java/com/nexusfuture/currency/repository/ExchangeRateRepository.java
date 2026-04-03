package com.nexusfuture.currency.repository;

import com.nexusfuture.currency.entity.ExchangeRate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ExchangeRateRepository extends JpaRepository<ExchangeRate, Long> {
    ExchangeRate findByDate(String date);
    List<ExchangeRate> findByDateOrderByIdDesc(String date);

}