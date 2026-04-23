package com.nexusfuture.currency.repository;

import com.nexusfuture.currency.entity.ExchangeRateHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface ExchangeRateHistoryRepository extends JpaRepository<ExchangeRateHistory, Long> {

    /**
     * 根据货币代码和日期范围查询历史记录
     */
    List<ExchangeRateHistory> findByCurrencyCodeAndRateDateBetweenOrderByRateDateAsc(String currencyCode, String startDate, String endDate);

    /**
     * 根据货币代码和一组日期查询记录
     */
    List<ExchangeRateHistory> findByCurrencyCodeAndRateDateIn(String currencyCode, List<String> rateDates);

    /**
     * 增量开发：根据货币代码和日期列表直接批量删除（原子操作）
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM ExchangeRateHistory e WHERE e.currencyCode = :currency AND e.rateDate IN :dates")
    void deleteByCurrencyCodeAndRateDateIn(@Param("currency") String currency, @Param("dates") List<String> dates);
}
