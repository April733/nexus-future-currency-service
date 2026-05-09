package com.nexusfuture.currency.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nexusfuture.currency.entity.ExchangeRate;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.Optional;

@Mapper
public interface ExchangeRateMapper extends BaseMapper<ExchangeRate> {

    /**
     * 查找数据库中最新的一条汇率记录
     */
    @Select("SELECT * FROM exchange_rate ORDER BY create_time DESC LIMIT 1")
    ExchangeRate findFirstByOrderByCreateTimeDesc();
}
