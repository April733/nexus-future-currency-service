package com.nexusfuture.currency.service;

import com.nexusfuture.currency.entity.User;
import com.nexusfuture.currency.repository.UserRepository;
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
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @PostConstruct
    public void migrateOldUsers() {
        List<User> allUsers = userRepository.findAll();
        int migratedCount = 0;
        
        for (User user : allUsers) {
            if (user.getUserId() == null || user.getUserId().trim().isEmpty()) {
                user.setUserId(generateUniqueUserId());
                userRepository.save(user);
                migratedCount++;
            }
        }
        
        if (migratedCount > 0) {
            System.out.println("✅ 已为 " + migratedCount + " 个旧用户生成 userId");
        }
    }

    public User createUser(String username, String email, String password) {
        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("用户名已存在，请选择其他用户名");
        }
        
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("邮箱已被注册，请使用其他邮箱");
        }
        
        User user = new User(username, email, passwordEncoder.encode(password));
        user.setUserId(generateUniqueUserId());
        return userRepository.save(user);
    }

    public User registerUser(String username, String email, String password) {
        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("用户名已存在，请选择其他用户名");
        }
        
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("邮箱已被注册，请使用其他邮箱");
        }
        
        User user = new User(username, email, passwordEncoder.encode(password));
        user.setUserId(generateUniqueUserId());
        return userRepository.save(user);
    }

    private String generateUniqueUserId() {
        String userId;
        do {
            userId = "USR-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        } while (userRepository.existsByUserId(userId));
        return userId;
    }

    public Optional<User> getUserByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public Optional<User> getUserByUserId(String userId) {
        return userRepository.findByUserId(userId);
    }

    public boolean checkPassword(User user, String rawPassword) {
        return passwordEncoder.matches(rawPassword, user.getPassword());
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public Optional<User> getUserById(Long id) {
        return userRepository.findById(id);
    }
    
    public boolean isUsernameExists(String username) {
        return userRepository.existsByUsername(username);
    }
    
    public boolean isEmailExists(String email) {
        return userRepository.existsByEmail(email);
    }
}