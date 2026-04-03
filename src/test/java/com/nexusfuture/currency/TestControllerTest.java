package com.nexusfuture.currency;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

public class TestControllerTest {

    // 固定用 RestTemplate 调用接口
    private static final RestTemplate restTemplate = new RestTemplate();
    private static final String BASE_URL = "http://localhost:8080/api/test/";

    public static void main(String[] args) {
        System.out.println("==================== 测试 GET 接口 ====================");
        testGetHello();

        System.out.println("\n==================== 测试 POST 接口 ====================");
        testPostLogin();
    }

    // 测试 GET /api/hello
    private static void testGetHello() {
        String url = BASE_URL + "/hello";

        // 入参：无
        System.out.println("【GET 入参】无");

        // 发送请求
        String result = restTemplate.getForObject(url, String.class);

        // 打印出参
        System.out.println("【GET 出参】" + result);
    }

    // 测试 POST /api/login
    private static void testPostLogin() {
        String url = BASE_URL + "/login";

        // 1. 构造入参
        Map<String, String> params = new HashMap<>();
        params.put("username", "test");
        params.put("password", "123456");

        // 打印入参
        System.out.println("【POST 入参】" + params);

        // 2. 请求头（JSON）
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // 3. 发送请求
        HttpEntity<Map<String, String>> request = new HttpEntity<>(params, headers);
        String result = restTemplate.exchange(url, HttpMethod.POST, request, String.class).getBody();

        // 打印出参
        System.out.println("【POST 出参】" + result);
    }
}

