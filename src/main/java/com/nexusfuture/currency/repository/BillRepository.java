package com.nexusfuture.currency.repository;

import com.nexusfuture.currency.entity.Bill;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface BillRepository extends JpaRepository<Bill, Long> {

    /**
     * 分页查询用户的账单列表（只返回启用且未删除的）
     */
    Page<Bill> findByUserIdAndStatusTrueAndIsDeletedFalseOrderByBillDateDesc(
            String userId, Pageable pageable);

    /**
     * 按货币筛选
     */
    List<Bill> findByUserIdAndCurrencyAndStatusTrueAndIsDeletedFalseOrderByBillDateDesc(
            String userId, String currency);

    /**
     * 按分类筛选
     */
    List<Bill> findByUserIdAndCategoryAndStatusTrueAndIsDeletedFalseOrderByBillDateDesc(
            String userId, String category);

    /**
     * 按日期范围查询
     */
    List<Bill> findByUserIdAndBillDateBetweenAndStatusTrueAndIsDeletedFalseOrderByBillDateDesc(
            String userId, String startDate, String endDate);

    /**
     * 根据ID和userId查询（确保只能操作自己的账单）
     */
    Bill findByIdAndUserIdAndIsDeletedFalse(Long id, String userId);
}
