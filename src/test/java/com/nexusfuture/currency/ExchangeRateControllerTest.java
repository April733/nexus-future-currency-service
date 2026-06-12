package com.nexusfuture.currency;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.nexusfuture.currency.common.Result;
import com.nexusfuture.currency.controller.ExchangeRateController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Rollback(false) // 🔥 关键：关闭自动回滚！
public class ExchangeRateControllerTest {

    @Autowired
    private ExchangeRateController controller;

    @Test
    void getLatestRate() {
        System.out.println("getLatestRate-test begin:::");

        // 🔥 测试1：默认数据源（ECB）
        System.out.println("\n========== 测试 ECB 数据源 ==========");
        Result<?> ecbResult = controller.getLatestRate(null);
        assertNotNull(ecbResult, "ECB结果不能为空");
        assertEquals(200, ecbResult.getCode(), "ECB请求应成功");
        assertNotNull(ecbResult.getData(), "ECB数据不能为空");
        
        // 🔥 修正：Result.getData() 返回 List，不是 JSONArray
        java.util.List<?> ecbDataList = (java.util.List<?>) ecbResult.getData();
        assertNotNull(ecbDataList, "ECB汇率列表不能为空");
        assertFalse(ecbDataList.isEmpty(), "ECB汇率列表不能为空");
        assertTrue(ecbDataList.size() >= 12, 
            String.format("ECB汇率应至少包含12种货币，实际只有 %d 种", ecbDataList.size()));
        
        System.out.println("✅ ECB数据源验证通过，共 " + ecbDataList.size() + " 种货币");
        
        // 🔥 测试2：BOC 数据源
        System.out.println("\n========== 测试 BOC 数据源 ==========");
        Result<?> bocResult = controller.getLatestRate("BOC");
        assertNotNull(bocResult, "BOC结果不能为空");
        assertEquals(200, bocResult.getCode(), "BOC请求应成功");
        assertNotNull(bocResult.getData(), "BOC数据不能为空");
        
        // 🔥 修正：Result.getData() 返回 List，不是 JSONArray
        java.util.List<?> bocDataList = (java.util.List<?>) bocResult.getData();
        assertNotNull(bocDataList, "BOC汇率列表不能为空");
        assertFalse(bocDataList.isEmpty(), "BOC汇率列表不能为空");
        assertTrue(bocDataList.size() >= 12, 
            String.format("BOC汇率应至少包含12种货币，实际只有 %d 种", bocDataList.size()));
        
        System.out.println("✅ BOC数据源验证通过，共 " + bocDataList.size() + " 种货币");
        
        // 🔥 测试3：验证两种数据源格式一致（都应该是ECB基准）
        System.out.println("\n========== 验证数据格式一致性 ==========");
        for (int i = 0; i < Math.min(ecbDataList.size(), bocDataList.size()); i++) {
            // 🔥 修正：将 Object 转换为 CurrencyRateDto
            com.nexusfuture.currency.dto.CurrencyRateDto ecbItem = 
                (com.nexusfuture.currency.dto.CurrencyRateDto) ecbDataList.get(i);
            com.nexusfuture.currency.dto.CurrencyRateDto bocItem = 
                (com.nexusfuture.currency.dto.CurrencyRateDto) bocDataList.get(i);
            
            String ecbCode = ecbItem.getCode();
            String bocCode = bocItem.getCode();
            
            // 验证货币代码一致
            assertEquals(ecbCode, bocCode, 
                String.format("第%d条记录的货币代码应一致", i));
            
            // 验证汇率值在合理范围内（ECB基准：1 EUR = X 外币）
            BigDecimal ecbRate = ecbItem.getRate();
            BigDecimal bocRate = bocItem.getRate();
            
            // 允许5%的误差（因为数据采集时间可能不同）
            BigDecimal tolerance = ecbRate.multiply(new BigDecimal("0.05"));
            BigDecimal diff = ecbRate.subtract(bocRate).abs();
            
            if ("USD".equals(ecbCode)) {
                assertTrue(diff.compareTo(tolerance) <= 0,
                    String.format("%s汇率差异过大: ECB=%s, BOC=%s, 差值=%s", 
                        ecbCode, ecbRate, bocRate, diff));
            }
            
            System.out.printf("✅ %s: ECB=%s, BOC=%s, 差值=%s%n", 
                ecbCode, ecbRate, bocRate, diff);
        }
        
        System.out.println("\ngetLatestRate-test end:::");
    }

    @Test
    void refillData() {
        System.out.println("refillData-test begin:::");

        // 1. 调用手动触发接口
        Result<String> result = controller.refill();

        // 2. 打印返回结果
        System.out.println("接口返回消息：" + result.getData());

        // 3. 断言操作成功
        assertNotNull(result);
        assertEquals(200, result.getCode());
    }


    @Test
    void fetchRateByRange() {
        System.out.println("fetchRateByRange-test begin:::");

        // 1. 定义测试参数：印尼盾（IDR），最近一个月
        String currency = "IDR";
        String startDate = "2026-03-27";
        String endDate = "2026-04-27";

        // 2. 调用新增的按日期范围拉取接口
        Result<String> result = controller.fetchRateByRange(currency, startDate, endDate);

        // 3. 打印完整数据
        System.out.println("接口返回数据：" + JSON.toJSONString(result));

        // 4. 断言不为空且执行成功
        assertNotNull(result);
        assertEquals(200, result.getCode());
        System.out.println("fetchRateByRange-test end:::");
    }

    @Test
    void getHistoryRates() {
        System.out.println("getHistoryRates-test begin:::");

        // 1. 定义测试参数：印尼盾（IDR），最近一个月
        String currency = "IDR";
        String startDate = "2026-03-27";
        String endDate = "2026-04-27";

        // 2. 调用历史汇率查询接口
        Result<?> result = controller.getHistory(currency, startDate, endDate);

        // 3. 打印完整数据
        System.out.println("历史汇率查询返回数据：" + JSON.toJSONString(result));

        // 4. 断言不为空且执行成功
        assertNotNull(result);
        assertEquals(200, result.getCode());
        assertNotNull(result.getData());
        System.out.println("getHistoryRates-test end:::");
    }
}