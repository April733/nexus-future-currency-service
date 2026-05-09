package com.nexusfuture.currency.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nexusfuture.currency.entity.ExchangeRateHistory;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ExchangeRateHistoryMapper extends BaseMapper<ExchangeRateHistory> {

    /**
     * 根据货币代码和日期范围查询历史记录
     */
    @Select("SELECT * FROM exchange_rate_history WHERE currency_code = #{currencyCode} AND rate_date BETWEEN #{startDate} AND #{endDate} ORDER BY rate_date ASC")
    List<ExchangeRateHistory> findByCurrencyCodeAndRateDateBetweenOrderByRateDateAsc(
            @Param("currencyCode") String currencyCode,
            @Param("startDate") String startDate,
            @Param("endDate") String endDate);

    /**
     * 根据货币代码和一组日期查询记录
     */
    @Select("<script>" +
            "SELECT * FROM exchange_rate_history WHERE currency_code = #{currencyCode} AND rate_date IN " +
            "<foreach collection='rateDates' item='date' open='(' separator=',' close=')'>" +
            "#{date}" +
            "</foreach>" +
            "</script>")
    List<ExchangeRateHistory> findByCurrencyCodeAndRateDateIn(
            @Param("currencyCode") String currencyCode,
            @Param("rateDates") List<String> rateDates);

    /**
     * 根据货币代码和日期列表直接批量删除
     */
    @Delete("<script>" +
            "DELETE FROM exchange_rate_history WHERE currency_code = #{currency} AND rate_date IN " +
            "<foreach collection='dates' item='date' open='(' separator=',' close=')'>" +
            "#{date}" +
            "</foreach>" +
            "</script>")
    void deleteByCurrencyCodeAndRateDateIn(
            @Param("currency") String currency,
            @Param("dates") List<String> dates);
}
