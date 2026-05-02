package com.nexusfuture.currency.controller;

import com.nexusfuture.currency.common.Result;
import com.nexusfuture.currency.dto.BillCreateRequest;
import com.nexusfuture.currency.dto.BillResponse;
import com.nexusfuture.currency.dto.BillUpdateRequest;
import com.nexusfuture.currency.service.BillService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bill")
@RequiredArgsConstructor
@Tag(name = "账单管理", description = "提供账单的增删改查功能")
public class BillController {

    private final BillService billService;

    @Operation(summary = "创建账单", description = "创建一笔新的账单记录")
    @PostMapping
    public Result<BillResponse> createBill(
            HttpServletRequest request,
            @Valid @RequestBody BillCreateRequest req) {
        String userId = (String) request.getAttribute("userId");
        BillResponse response = billService.createBill(userId, req);
        return Result.success(response);
    }

    @Operation(summary = "分页查询账单列表", description = "获取当前用户的账单列表（分页）")
    @GetMapping("/list")
    public Result<Page<BillResponse>> getBillList(
            HttpServletRequest request,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        String userId = (String) request.getAttribute("userId");
        Page<BillResponse> list = billService.getBillList(userId, page, size);
        return Result.success(list);
    }

    @Operation(summary = "查询账单详情", description = "根据账单ID查询详细信息")
    @GetMapping("/{id}")
    public Result<BillResponse> getBillDetail(
            HttpServletRequest request,
            @Parameter(description = "账单ID（创建账单后返回的ID）", example = "1", required = true)
            @PathVariable Long id) {
        String userId = (String) request.getAttribute("userId");
        BillResponse response = billService.getBillDetail(userId, id);
        return Result.success(response);
    }

    @Operation(summary = "更新账单", description = "修改账单信息")
    @PutMapping("/{id}")
    public Result<BillResponse> updateBill(
            HttpServletRequest request,
            @Parameter(description = "账单ID（要更新的账单ID）", example = "1", required = true)
            @PathVariable Long id,
            @RequestBody BillUpdateRequest req) {
        String userId = (String) request.getAttribute("userId");
        BillResponse response = billService.updateBill(userId, id, req);
        return Result.success(response);
    }

    @Operation(summary = "删除账单", description = "逻辑删除账单（不会真正删除数据）")
    @DeleteMapping("/{id}")
    public Result<Void> deleteBill(
            HttpServletRequest request,
            @Parameter(description = "账单ID（要删除的账单ID）", example = "1", required = true)
            @PathVariable Long id) {
        String userId = (String) request.getAttribute("userId");
        billService.deleteBill(userId, id);
        return Result.success(null);
    }

    @Operation(summary = "切换账单状态", description = "启用或关闭账单显示")
    @PatchMapping("/{id}/toggle-status")
    public Result<BillResponse> toggleStatus(
            HttpServletRequest request,
            @Parameter(description = "账单ID（要切换状态的账单ID）", example = "1", required = true)
            @PathVariable Long id) {
        String userId = (String) request.getAttribute("userId");
        BillResponse response = billService.toggleBillStatus(userId, id);
        return Result.success(response);
    }

    @Operation(summary = "按分类查询账单", description = "查询指定分类的所有账单")
    @GetMapping("/category/{category}")
    public Result<List<BillResponse>> getByCategory(
            HttpServletRequest request,
            @PathVariable String category) {
        String userId = (String) request.getAttribute("userId");
        List<BillResponse> list = billService.getBillsByCategory(userId, category);
        return Result.success(list);
    }

    @Operation(summary = "按货币查询账单", description = "查询指定货币的所有账单")
    @GetMapping("/currency/{currency}")
    public Result<List<BillResponse>> getByCurrency(
            HttpServletRequest request,
            @PathVariable String currency) {
        String userId = (String) request.getAttribute("userId");
        List<BillResponse> list = billService.getBillsByCurrency(userId, currency);
        return Result.success(list);
    }

}
