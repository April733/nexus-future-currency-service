package com.nexusfuture.currency;

import com.nexusfuture.currency.common.Result;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class UserControllerTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    private String baseUrl;
    private String token;
    private String testUserId;

    @BeforeEach
    void setUp() {
        baseUrl = "http://localhost:" + port + "/api/users";
        
        // 步骤1：注册测试用户
        registerTestUser();
        
        // 步骤2：登录获取Token和用户ID
        loginAndGetToken();
    }

    /**
     * 注册测试用户
     */
    private void registerTestUser() {
        String url = baseUrl + "/register";
        
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("username", "test_user");
        params.add("email", "test@example.com");
        params.add("password", "123456");

        ResponseEntity<Result> response = restTemplate.postForEntity(url, params, Result.class);
        assertEquals(200, response.getStatusCode().value(), "注册应该成功");
    }

    /**
     * 登录并获取Token
     */
    private void loginAndGetToken() {
        String url = baseUrl + "/login";
        
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("username", "test_user");
        params.add("password", "123456");

        ResponseEntity<Result> response = restTemplate.postForEntity(url, params, Result.class);
        assertEquals(200, response.getStatusCode().value(), "登录应该成功");
        
        Map<String, String> data = (Map<String, String>) response.getBody().getData();
        token = data.get("token");
        assertNotNull(token, "Token不应该为空");
    }

    @Test
    @DisplayName("✅ 测试注册新用户")
    void testRegister() {
        String url = baseUrl + "/register";
        
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("username", "new_user_001");
        params.add("email", "new001@example.com");
        params.add("password", "password123");

        ResponseEntity<Result> response = restTemplate.postForEntity(url, params, Result.class);
        
        assertEquals(200, response.getStatusCode().value());
        assertEquals("注册成功", response.getBody().getData());
    }

    @Test
    @DisplayName("✅ 测试登录成功")
    void testLoginSuccess() {
        String url = baseUrl + "/login";
        
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("username", "test_user");
        params.add("password", "123456");

        ResponseEntity<Result> response = restTemplate.postForEntity(url, params, Result.class);
        
        assertEquals(200, response.getStatusCode().value());
        Map<String, String> data = (Map<String, String>) response.getBody().getData();
        assertTrue(data.containsKey("token"), "响应应该包含token");
    }

    @Test
    @DisplayName("❌ 测试登录失败-用户不存在")
    void testLoginUserNotFound() {
        String url = baseUrl + "/login";
        
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("username", "nonexistent_user");
        params.add("password", "123456");

        ResponseEntity<Result> response = restTemplate.postForEntity(url, params, Result.class);
        
        assertEquals(200, response.getStatusCode().value());
        assertEquals(400, response.getBody().getCode());
        assertEquals("用户不存在，请先注册", response.getBody().getMsg());
    }

    @Test
    @DisplayName("❌ 测试登录失败-密码错误")
    void testLoginFailed() {
        String url = baseUrl + "/login";
        
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("username", "test_user");
        params.add("password", "wrong_password");

        ResponseEntity<Result> response = restTemplate.postForEntity(url, params, Result.class);
        
        assertEquals(200, response.getStatusCode().value());
        assertEquals(401, response.getBody().getCode());
        assertEquals("密码错误，请重试", response.getBody().getMsg());
    }

    @Test
    @DisplayName("✅ 测试根据ID获取用户-携带Token")
    void testGetUserByIdWithToken() {
        // 先通过登录接口获取用户信息来拿到userId
        String loginUrl = baseUrl + "/login";
        MultiValueMap<String, String> loginParams = new LinkedMultiValueMap<>();
        loginParams.add("username", "test_user");
        loginParams.add("password", "123456");
        
        // 先获取一个已认证用户的ID（通过查询接口）
        String url = baseUrl + "/1";
        
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);
        HttpEntity<?> entity = new HttpEntity<>(headers);

        ResponseEntity<Result> response = restTemplate.exchange(url, HttpMethod.GET, entity, Result.class);
        
        assertEquals(200, response.getStatusCode().value());
        assertEquals(200, response.getBody().getCode());
        assertNotNull(response.getBody().getData(), "应该返回用户数据");
    }

    @Test
    @DisplayName("❌ 测试根据ID获取用户-无Token")
    void testGetUserByIdWithoutToken() {
        String url = baseUrl + "/1";
        
        ResponseEntity<Result> response = restTemplate.getForEntity(url, Result.class);
        
        assertEquals(401, response.getStatusCode().value());
        assertEquals(401, response.getBody().getCode());
        assertEquals("未授权：请先登录获取Token", response.getBody().getMsg());
    }

    @Test
    @DisplayName("❌ 测试获取不存在的用户")
    void testGetUserByIdNotFound() {
        String url = baseUrl + "/99999";
        
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);
        HttpEntity<?> entity = new HttpEntity<>(headers);

        ResponseEntity<Result> response = restTemplate.exchange(url, HttpMethod.GET, entity, Result.class);
        
        assertEquals(200, response.getStatusCode().value());
        assertEquals(404, response.getBody().getCode());
        assertEquals("用户不存在，请检查用户ID是否正确", response.getBody().getMsg());
    }

    @Test
    @DisplayName("❌ 测试无效Token")
    void testInvalidToken() {
        String url = baseUrl + "/1";
        
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer invalid_token_here");
        HttpEntity<?> entity = new HttpEntity<>(headers);

        ResponseEntity<Result> response = restTemplate.exchange(url, HttpMethod.GET, entity, Result.class);
        
        assertEquals(401, response.getStatusCode().value());
    }

    @Test
    @DisplayName("✅ 测试用户名重复注册")
    void testRegisterDuplicateUsername() {
        String url = baseUrl + "/register";
        
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("username", "test_user");
        params.add("email", "another@example.com");
        params.add("password", "123456");

        ResponseEntity<Result> response = restTemplate.postForEntity(url, params, Result.class);
        
        assertEquals(200, response.getStatusCode().value());
        assertEquals(400, response.getBody().getCode());
        assertEquals("用户名已存在，请选择其他用户名", response.getBody().getMsg());
    }

    @Test
    @DisplayName("✅ 测试邮箱重复注册")
    void testRegisterDuplicateEmail() {
        String url = baseUrl + "/register";
        
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("username", "another_user_002");
        params.add("email", "test@example.com");
        params.add("password", "123456");

        ResponseEntity<Result> response = restTemplate.postForEntity(url, params, Result.class);
        
        assertEquals(200, response.getStatusCode().value());
        assertEquals(400, response.getBody().getCode());
        assertEquals("邮箱已被注册，请使用其他邮箱", response.getBody().getMsg());
    }
}
