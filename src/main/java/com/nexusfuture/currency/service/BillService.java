package com.nexusfuture.currency.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nexusfuture.currency.dto.BillCreateRequest;
import com.nexusfuture.currency.dto.BillResponse;
import com.nexusfuture.currency.dto.BillUpdateRequest;
import com.nexusfuture.currency.entity.Bill;
import com.nexusfuture.currency.mapper.BillMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
