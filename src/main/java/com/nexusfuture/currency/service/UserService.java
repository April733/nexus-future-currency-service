package com.nexusfuture.currency.service;

import com.nexusfuture.currency.entity.User;
import com.nexusfuture.currency.repository.UserRepository;
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

    public User createUser(String username, String email, String password) {
        // 验证用户名唯一性
        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("用户名已存在，请选择其他用户名");
        }
        
        // 验证邮箱唯一性
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("邮箱已被注册，请使用其他邮箱");
        }
        
        User user = new User(username, email, passwordEncoder.encode(password));
        user.setUserId(generateUniqueUserId());
        return userRepository.save(user);
    }

    public User registerUser(String username, String email, String password) {
        // 验证用户名唯一性
        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("用户名已存在，请选择其他用户名");
        }
        
        // 验证邮箱唯一性
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("邮箱已被注册，请使用其他邮箱");
        }
        
        User user = new User(username, email, passwordEncoder.encode(password));
        user.setUserId(generateUniqueUserId());
        return userRepository.save(user);
    }

    /**
     * 生成唯一的用户ID
     */
    private String generateUniqueUserId() {
        String userId;
        do {
            userId = "USR-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        } while (userRepository.existsByUserId(userId));
        return userId;
    }

    /**
     * 根据用户名查询用户
     */
    public Optional<User> getUserByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    /**
     * 根据用户ID查询用户
     */
    public Optional<User> getUserByUserId(String userId) {
        return userRepository.findByUserId(userId);
    }

    /**
     * 校验密码是否正确
     */
    public boolean checkPassword(User user, String rawPassword) {
        return passwordEncoder.matches(rawPassword, user.getPassword());
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public Optional<User> getUserById(Long id) {
        return userRepository.findById(id);
    }
    
    /**
     * 检查用户名是否已存在
     */
    public boolean isUsernameExists(String username) {
        return userRepository.existsByUsername(username);
    }
    
    /**
     * 检查邮箱是否已存在
     */
    public boolean isEmailExists(String email) {
        return userRepository.existsByEmail(email);
    }
}