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
}