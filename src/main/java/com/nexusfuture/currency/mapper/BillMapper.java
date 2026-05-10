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

    /**
     * 查询指定月份的账单（按日期降序）
     */
    @Select("SELECT * FROM bill WHERE user_id = #{userId} AND status = 1 AND is_deleted = 0 AND bill_date LIKE CONCAT(#{yearMonth}, '%') ORDER BY bill_date DESC")
    List<Bill> findByUserIdAndYearMonthOrderByBillDateDesc(
            @Param("userId") String userId,
            @Param("yearMonth") String yearMonth);

    /**
     * 查询用户最新的月份
     * 修复：完全移除别名 (as)，使用 GROUP BY 1 (位置索引)
     */
    @Select("SELECT DATE_FORMAT(bill_date, '%Y-%m') FROM bill WHERE user_id = #{userId} AND status = 1 AND is_deleted = 0 AND bill_date IS NOT NULL GROUP BY 1 ORDER BY 1 DESC LIMIT 1")
    String findLatestYearMonth(@Param("userId") String userId);

    /**
     * 查询指定月份之前的N个月份
     * 修复：完全移除别名，使用位置索引，确保 100% 兼容
     */
    @Select("SELECT DATE_FORMAT(bill_date, '%Y-%m') FROM bill WHERE user_id = #{userId} AND status = 1 AND is_deleted = 0 AND DATE_FORMAT(bill_date, '%Y-%m') < #{yearMonth} AND bill_date IS NOT NULL GROUP BY 1 ORDER BY 1 DESC LIMIT #{limit}")
    List<String> findPreviousYearMonths(
            @Param("userId") String userId,
            @Param("yearMonth") String yearMonth,
            @Param("limit") int limit);
}
