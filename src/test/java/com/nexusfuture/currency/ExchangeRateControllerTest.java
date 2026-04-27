package com.nexusfuture.currency;

import com.alibaba.fastjson2.JSON;
import com.nexusfuture.currency.common.Result;
import com.nexusfuture.currency.controller.ExchangeRateController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@Rollback(false) // 🔥 关键：关闭自动回滚！
public class ExchangeRateControllerTest {

    @Autowired
    private ExchangeRateController controller;

    @Test
    void getLatestRate() {
        System.out.println("getLatestRate-test begin:::");

        // 1. 接收接口返回的结果
        Result<?> result = controller.getLatestRate();

        // 2. 打印完整数据
        System.out.println("接口返回数据：" + JSON.toJSONString(result));

        // 3. 断言不为空
        assertNotNull(result);
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