package com.nexusfuture.currency.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nexusfuture.currency.entity.BocExchangeRate;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface BocExchangeRateMapper extends BaseMapper<BocExchangeRate> {

    /**
     * 根据货币代码查询最新一条记录
     */
    @Select("SELECT * FROM boc_exchange_rate WHERE currency_code = #{currencyCode} ORDER BY create_time DESC LIMIT 1")
    BocExchangeRate findLatestByCurrencyCode(@Param("currencyCode") String currencyCode);

    /**
     * 查询指定日期范围内的汇率
     */
    @Select("SELECT * FROM boc_exchange_rate WHERE currency_code = #{currencyCode} AND rate_date BETWEEN #{startDate} AND #{endDate} ORDER BY rate_date ASC")
    List<BocExchangeRate> findByCurrencyCodeAndRateDateBetween(
            @Param("currencyCode") String currencyCode,
            @Param("startDate") String startDate,
            @Param("endDate") String endDate);

    /**
     * 查询所有不同的日期
     */
    @Select("SELECT DISTINCT rate_date FROM boc_exchange_rate ORDER BY rate_date DESC")
    List<String> findDistinctRateDates();

    /**
     * 删除指定日期的数据
     */
    @Delete("DELETE FROM boc_exchange_rate WHERE rate_date = #{rateDate}")
    int deleteByRateDate(@Param("rateDate") String rateDate);

    /**
     * 删除指定日期之前的数据
     */
    @Delete("DELETE FROM boc_exchange_rate WHERE rate_date < #{rateDate}")
    int deleteByRateDateBefore(@Param("rateDate") String rateDate);

    /**
     * 查询最新的 N 条记录
     */
    @Select("SELECT * FROM boc_exchange_rate ORDER BY create_time DESC LIMIT #{limit}")
    List<BocExchangeRate> findTopNByOrderByCreateTimeDesc(@Param("limit") int limit);
}
