package com.nexusfuture.currency.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nexusfuture.currency.dto.BillCreateRequest;
import com.nexusfuture.currency.dto.BillResponse;
import com.nexusfuture.currency.dto.BillUpdateRequest;
import com.nexusfuture.currency.dto.BillGroupedByDateResponse;
import com.nexusfuture.currency.dto.BillGroupedPageResponse;
import com.nexusfuture.currency.dto.BillMonthlyPageRequest;
import com.nexusfuture.currency.entity.Bill;
import com.nexusfuture.currency.mapper.BillMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BillService {

    private final BillMapper billMapper;

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 创建账单
     */
    @Transactional
    public BillResponse createBill(String userId, BillCreateRequest request) {
        // 验证分类是否合法
        validateCategory(request.getCategory());

        // 验证货币代码是否合法
        validateCurrency(request.getCurrency());

        Bill bill = new Bill();
        bill.setUserId(userId);
        bill.setAmount(request.getAmount());
        bill.setCurrency(request.getCurrency());
        bill.setCategory(request.getCategory());
        bill.setRemark(request.getRemark());
        bill.setBillDate(request.getBillDate());
        bill.setStatus(true);
        bill.setIsDeleted(false);

        String now = LocalDateTime.now().format(FORMATTER);
        bill.setCreateTime(now);
        bill.setUpdateTime(now);

        billMapper.insert(bill);
        log.info("用户 {} 创建账单成功，ID: {}", userId, bill.getId());

        return convertToResponse(bill);
    }

    /**
     * 分页查询账单列表
     */
    public IPage<BillResponse> getBillList(String userId, int page, int size) {
        Page<Bill> mpPage = new Page<>(page, size);
        IPage<Bill> bills = billMapper.findByUserIdAndStatusTrueAndIsDeletedFalseOrderByBillDateDesc(
                mpPage, userId);

        return bills.convert(this::convertToResponse);
    }

    /**
     * 查询账单详情
     */
    public BillResponse getBillDetail(String userId, Long id) {
        log.info("正在查询账单详情: userId={}, billId={}", userId, id);
        Bill bill = billMapper.findByIdAndUserIdAndIsDeletedFalse(id, userId);
        if (bill == null) {
            throw new RuntimeException("账单不存在或无权访问");
        }
        return convertToResponse(bill);
    }

    /**
     * 更新账单
     */
    @Transactional
    public BillResponse updateBill(String userId, Long id, BillUpdateRequest request) {
        Bill bill = billMapper.findByIdAndUserIdAndIsDeletedFalse(id, userId);
        if (bill == null) {
            throw new RuntimeException("账单不存在或无权访问");
        }

        // 选择性更新
        if (request.getAmount() != null) {
            bill.setAmount(request.getAmount());
        }
        if (request.getCurrency() != null) {
            validateCurrency(request.getCurrency());
            bill.setCurrency(request.getCurrency());
        }
        if (request.getCategory() != null) {
            validateCategory(request.getCategory());
            bill.setCategory(request.getCategory());
        }
        if (request.getRemark() != null) {
            bill.setRemark(request.getRemark());
        }
        if (request.getBillDate() != null) {
            bill.setBillDate(request.getBillDate());
        }
        if (request.getStatus() != null) {
            bill.setStatus(request.getStatus());
        }

        bill.setUpdateTime(LocalDateTime.now().format(FORMATTER));
        billMapper.updateById(bill);

        log.info("用户 {} 更新账单成功，ID: {}", userId, id);
        return convertToResponse(bill);
    }

    /**
     * 逻辑删除账单
     */
    @Transactional
    public void deleteBill(String userId, Long id) {
        Bill bill = billMapper.findByIdAndUserIdAndIsDeletedFalse(id, userId);
        if (bill == null) {
            throw new RuntimeException("账单不存在或无权访问");
        }

        bill.setIsDeleted(true);
        bill.setUpdateTime(LocalDateTime.now().format(FORMATTER));
        billMapper.updateById(bill);

        log.info("用户 {} 删除账单成功，ID: {}", userId, id);
    }

    /**
     * 切换账单状态（启用/关闭）
     */
    @Transactional
    public BillResponse toggleBillStatus(String userId, Long id) {
        Bill bill = billMapper.findByIdAndUserIdAndIsDeletedFalse(id, userId);
        if (bill == null) {
            throw new RuntimeException("账单不存在或无权访问");
        }

        bill.setStatus(!bill.getStatus());
        bill.setUpdateTime(LocalDateTime.now().format(FORMATTER));
        billMapper.updateById(bill);

        log.info("用户 {} 切换账单状态，ID: {}, 新状态: {}", userId, id, bill.getStatus());
        return convertToResponse(bill);
    }

    /**
     * 按分类查询
     */
    public List<BillResponse> getBillsByCategory(String userId, String category) {
        List<Bill> bills = billMapper
                .findByUserIdAndCategoryAndStatusTrueAndIsDeletedFalseOrderByBillDateDesc(
                        userId, category);
        return bills.stream().map(this::convertToResponse).collect(Collectors.toList());
    }

    /**
     * 按货币查询
     */
    public List<BillResponse> getBillsByCurrency(String userId, String currency) {
        List<Bill> bills = billMapper
                .findByUserIdAndCurrencyAndStatusTrueAndIsDeletedFalseOrderByBillDateDesc(
                        userId, currency);
        return bills.stream().map(this::convertToResponse).collect(Collectors.toList());
    }

    /**
     * 分页查询按日期分组的账单列表
     */
    public BillGroupedPageResponse getGroupedBillList(String userId, int page, int size) {
        Page<Bill> mpPage = new Page<>(page, size);
        IPage<Bill> bills = billMapper.findByUserIdAndStatusTrueAndIsDeletedFalseOrderByBillDateDesc(
                mpPage, userId);

        List<Bill> billList = bills.getRecords();

        // 按日期分组（关键修复：使用 substring 提取日期部分）
        List<BillGroupedByDateResponse> groupedBills = billList.stream()
                .collect(Collectors.groupingBy(
                        bill -> bill.getBillDate().substring(0, 10),  // 提取 "yyyy-MM-dd"
                        Collectors.toList()
                ))
                .entrySet().stream()
                .sorted((e1, e2) -> e2.getKey().compareTo(e1.getKey()))
                .map(entry -> {
                    String date = entry.getKey();
                    List<Bill> dayBills = entry.getValue();

                    List<BillGroupedByDateResponse.BillItemResponse> billItems = dayBills.stream()
                            .map(this::convertToItemResponse)
                            .collect(Collectors.toList());

                    BigDecimal dailyTotal = dayBills.stream()
                            .map(Bill::getAmount)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    return BillGroupedByDateResponse.builder()
                            .date(date)
                            .dailyTotal(dailyTotal)
                            .bills(billItems)
                            .build();
                })
                .collect(Collectors.toList());

        // 计算总金额
        BigDecimal total = billList.stream()
                .map(Bill::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return BillGroupedPageResponse.builder()
                .groupedBills(groupedBills)
                .total(total)
                .currentYearMonth(null)
                .nextYearMonth(null)
                .hasMore(bills.getCurrent() < bills.getPages())
                .build();
    }

    /**
     * 按月份分页查询账单列表（推荐方案）
     */
    public BillGroupedPageResponse getMonthlyGroupedBillList(String userId, String yearMonth, int months) {
        // 🔒 安全拦截：防止脏数据或非标准格式传入数据库
        if (yearMonth != null && !yearMonth.matches("\\d{4}-\\d{2}")) {
            throw new IllegalArgumentException("月份格式错误，必须为 yyyy-MM");
        }

        // 1. 如果未指定月份，获取最新月份
        if (yearMonth == null || yearMonth.isEmpty()) {
            String latest = billMapper.findLatestYearMonth(userId);
            if (latest == null) {
                return BillGroupedPageResponse.builder()
                        .groupedBills(List.of())
                        .total(BigDecimal.ZERO)
                        .hasMore(false)
                        .build();
            }
            yearMonth = latest;
        }

        // 2. 查询指定月份及之前N个月的账单
        List<String> yearMonths = new java.util.ArrayList<>();
        yearMonths.add(yearMonth);
        
        if (months > 1) {
            List<String> previousMonths = billMapper.findPreviousYearMonths(userId, yearMonth, months - 1);
            yearMonths.addAll(previousMonths);
        }

        // 3. 收集所有月份的账单
        List<Bill> allBills = new java.util.ArrayList<>();
        for (String ym : yearMonths) {
            List<Bill> monthBills = billMapper.findByUserIdAndYearMonthOrderByBillDateDesc(userId, ym);
            allBills.addAll(monthBills);
        }

        // 4. 按日期分组（关键修复：使用 substring 提取日期部分）
        List<BillGroupedByDateResponse> groupedBills = allBills.stream()
                .collect(Collectors.groupingBy(
                        bill -> bill.getBillDate().substring(0, 10),  // 提取 "yyyy-MM-dd"
                        Collectors.toList()
                ))
                .entrySet().stream()
                .sorted((e1, e2) -> e2.getKey().compareTo(e1.getKey()))
                .map(entry -> {
                    String date = entry.getKey();
                    List<Bill> dayBills = entry.getValue();

                    List<BillGroupedByDateResponse.BillItemResponse> billItems = dayBills.stream()
                            .map(this::convertToItemResponse)
                            .collect(Collectors.toList());

                    BigDecimal dailyTotal = dayBills.stream()
                            .map(Bill::getAmount)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    return BillGroupedByDateResponse.builder()
                            .date(date)
                            .dailyTotal(dailyTotal)
                            .bills(billItems)
                            .build();
                })
                .collect(Collectors.toList());

        // 5. 计算总金额
        BigDecimal total = allBills.stream()
                .map(Bill::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 6. 判断是否有更多数据
        String latestMonth = billMapper.findLatestYearMonth(userId);
        boolean hasMore = !yearMonth.equals(latestMonth) || 
                         (months > 0 && !yearMonths.get(yearMonths.size() - 1).equals(latestMonth));

        // 7. 计算下一个游标（上一个月份）
        String nextYearMonth = null;
        if (hasMore) {
            // 获取当前最早月份的前一个月
            String earliestMonth = yearMonths.get(yearMonths.size() - 1);
            List<String> prevMonths = billMapper.findPreviousYearMonths(userId, earliestMonth, 1);
            if (!prevMonths.isEmpty()) {
                nextYearMonth = prevMonths.get(0);
            }
        }

        return BillGroupedPageResponse.builder()
                .groupedBills(groupedBills)
                .total(total)
                .currentYearMonth(yearMonth)
                .nextYearMonth(nextYearMonth)
                .hasMore(hasMore)
                .build();
    }

    // ========== 私有方法 ==========

    private BillResponse convertToResponse(Bill bill) {
        return BillResponse.builder()
                .id(bill.getId())
                .amount(bill.getAmount())
                .currency(bill.getCurrency())
                .category(bill.getCategory())
                .remark(bill.getRemark())
                .billDate(bill.getBillDate())
                .status(bill.getStatus())
                .createTime(bill.getCreateTime())
                .build();
    }

    private BillGroupedByDateResponse.BillItemResponse convertToItemResponse(Bill bill) {
        return BillGroupedByDateResponse.BillItemResponse.builder()
                .id(bill.getId())
                .amount(bill.getAmount())
                .currency(bill.getCurrency())
                .category(bill.getCategory())
                .remark(bill.getRemark())
                .billDate(bill.getBillDate())
                .status(bill.getStatus())
                .build();
    }

    private void validateCategory(String category) {
        List<String> validCategories = List.of("餐饮", "住宿", "交通", "购物", "其他");
        if (!validCategories.contains(category)) {
            throw new RuntimeException("无效的分类：" + category);
        }
    }

    private void validateCurrency(String currency) {
        // 可以复用项目中现有的货币枚举
        List<String> validCurrencies = List.of("CNY", "USD", "EUR", "GBP", "JPY");
        if (!validCurrencies.contains(currency)) {
            throw new RuntimeException("无效的货币代码：" + currency);
        }
    }
}
