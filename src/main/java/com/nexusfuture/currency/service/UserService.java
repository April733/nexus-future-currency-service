package com.nexusfuture.currency.service;

import com.nexusfuture.currency.entity.User;
import com.nexusfuture.currency.mapper.UserMapper;
import jakarta.annotation.PostConstruct;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class UserService {
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserMapper userMapper, PasswordEncoder passwordEncoder) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @PostConstruct
    public void migrateOldUsers() {
        List<User> allUsers = userMapper.selectList(null);
        int migratedCount = 0;
        
        for (User user : allUsers) {
            if (user.getUserId() == null || user.getUserId().trim().isEmpty()) {
                user.setUserId(generateUniqueUserId());
                userMapper.updateById(user);
                migratedCount++;
            }
        }
        
        if (migratedCount > 0) {
            System.out.println("✅ 已为 " + migratedCount + " 个旧用户生成 userId");
        }
    }

    public User createUser(String username, String email, String password) {
        if (userMapper.existsByUsername(username)) {
            throw new IllegalArgumentException("用户名已存在，请选择其他用户名");
        }
        
        if (userMapper.existsByEmail(email)) {
            throw new IllegalArgumentException("邮箱已被注册，请使用其他邮箱");
        }
        
        User user = new User(username, email, passwordEncoder.encode(password));
        user.setUserId(generateUniqueUserId());
        userMapper.insert(user);
        return user;
    }

    public User registerUser(String username, String email, String password) {
        if (userMapper.existsByUsername(username)) {
            throw new IllegalArgumentException("用户名已存在，请选择其他用户名");
        }
        
        if (userMapper.existsByEmail(email)) {
            throw new IllegalArgumentException("邮箱已被注册，请使用其他邮箱");
        }
        
        User user = new User(username, email, passwordEncoder.encode(password));
        user.setUserId(generateUniqueUserId());
        userMapper.insert(user);
        return user;
    }

    private String generateUniqueUserId() {
        String userId;
        do {
            userId = "USR-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        } while (userMapper.existsByUserId(userId));
        return userId;
    }

    public Optional<User> getUserByUsername(String username) {
        return Optional.ofNullable(userMapper.findByUsername(username));
    }

    public Optional<User> getUserByUserId(String userId) {
        return Optional.ofNullable(userMapper.findByUserId(userId));
    }

    public boolean checkPassword(User user, String rawPassword) {
        return passwordEncoder.matches(rawPassword, user.getPassword());
    }

    public List<User> getAllUsers() {
        return userMapper.selectList(null);
    }

    public Optional<User> getUserById(Long id) {
        return Optional.ofNullable(userMapper.selectById(id));
    }
    
    public boolean isUsernameExists(String username) {
        return userMapper.existsByUsername(username);
    }
    
    public boolean isEmailExists(String email) {
        return userMapper.existsByEmail(email);
    }
}