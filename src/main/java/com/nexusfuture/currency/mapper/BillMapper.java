package com.nexusfuture.currency.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nexusfuture.currency.entity.Bill;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface BillMapper extends BaseMapper<Bill> {

    /**
     * 分页查询用户的账单列表（只返回启用且未删除的）
     */
    @Select("SELECT * FROM bill WHERE user_id = #{userId} AND status = 1 AND is_deleted = 0 ORDER BY bill_date DESC")
    IPage<Bill> findByUserIdAndStatusTrueAndIsDeletedFalseOrderByBillDateDesc(
            Page<Bill> page,
            @Param("userId") String userId);

    /**
     * 按货币筛选
     */
    @Select("SELECT * FROM bill WHERE user_id = #{userId} AND currency = #{currency} AND status = 1 AND is_deleted = 0 ORDER BY bill_date DESC")
    List<Bill> findByUserIdAndCurrencyAndStatusTrueAndIsDeletedFalseOrderByBillDateDesc(
            @Param("userId") String userId,
            @Param("currency") String currency);

    /**
     * 按分类筛选
     */
    @Select("SELECT * FROM bill WHERE user_id = #{userId} AND category = #{category} AND status = 1 AND is_deleted = 0 ORDER BY bill_date DESC")
    List<Bill> findByUserIdAndCategoryAndStatusTrueAndIsDeletedFalseOrderByBillDateDesc(
            @Param("userId") String userId,
            @Param("category") String category);

    /**
     * 按日期范围查询
     */
    @Select("SELECT * FROM bill WHERE user_id = #{userId} AND bill_date BETWEEN #{startDate} AND #{endDate} AND status = 1 AND is_deleted = 0 ORDER BY bill_date DESC")
    List<Bill> findByUserIdAndBillDateBetweenAndStatusTrueAndIsDeletedFalseOrderByBillDateDesc(
            @Param("userId") String userId,
            @Param("startDate") String startDate,
            @Param("endDate") String endDate);

    /**
     * 根据ID和userId查询（确保只能操作自己的账单）
     */
    @Select("SELECT * FROM bill WHERE id = #{id} AND user_id = #{userId} AND is_deleted = 0 LIMIT 1")
    Bill findByIdAndUserIdAndIsDeletedFalse(
            @Param("id") Long id,
            @Param("userId") String userId);
}
