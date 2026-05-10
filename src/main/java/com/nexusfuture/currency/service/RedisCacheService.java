package com.nexusfuture.currency.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedisCacheService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    /**
     * 将一个对象存入缓存，并设置指定的生存时间（TTL）。
     *
     * @param key   缓存键
     * @param value 要缓存的对象，它将被序列化为 JSON 字符串
     * @param ttl   缓存的生存时间
     */
    public void set(String key, Object value, Duration ttl) {
        try {
            String jsonValue = objectMapper.writeValueAsString(value);
            redisTemplate.opsForValue().set(key, jsonValue, ttl);
            log.debug("Redis set. key={}, ttl={}", key, ttl);
        } catch (JsonProcessingException e) {
            log.error("序列化对象以便存入 Redis 失败, key={}", key, e);
            throw new IllegalStateException("Failed to serialize object for Redis cache", e);
        }
    }

    /**
     * 从缓存中获取一个对象。
     *
     * @param key       缓存键
     * @param valueType 期望返回的对象的 Class 类型
     * @param <T>       期望返回的对象的泛型
     * @return 如果找到，则返回包含对象的 {@link Optional}；否则返回空 Optional。
     */
    public <T> Optional<T> get(String key, Class<T> valueType) {
        try {
            Object value = redisTemplate.opsForValue().get(key);
            if (value == null) {
                log.debug("Redis cache miss. key={}", key);
                return Optional.empty();
            }
            
            T result;
            if (valueType.isInstance(value)) {
                result = valueType.cast(value);
            } else if (value instanceof String) {
                result = objectMapper.readValue((String) value, valueType);
            } else {
                result = objectMapper.convertValue(value, valueType);
            }
            
            log.debug("Redis cache hit. key={}", key);
            return Optional.of(result);
        } catch (JsonProcessingException e) {
            log.error("从 Redis 反序列化对象失败, key={}, targetType={}", key, valueType.getName(), e);
            // 如果反序列化失败，安全起见可以删除这个损坏的键
            delete(key);
            return Optional.empty();
        }
    }

    /**
     * 从缓存中删除一个或多个指定的键。
     *
     * @param keys 要删除的缓存键
     */
    public void delete(String... keys) {
        if (keys != null && keys.length > 0) {
            Long deletedCount = redisTemplate.delete(Set.of(keys));
            log.debug("Redis deleted {} keys. keys={}", deletedCount, keys);
        }
    }

    /**
     * 删除所有匹配指定前缀的键。
     * <p>
     * <b>注意：</b>在 Redis 中，{@code KEYS} 命令可能会阻塞服务器。
     * 生产环境中，如果键空间很大，应考虑使用 {@code SCAN} 命令代替。
     * 为简单起见，此处仍使用 {@code KEYS}。
     *
     * @param prefix 键的前缀
     */
    public void deleteByPrefix(String prefix) {
        Set<String> keysToDelete = redisTemplate.keys(prefix + "*");
        if (keysToDelete != null && !keysToDelete.isEmpty()) {
            Long deletedCount = redisTemplate.delete(keysToDelete);
            log.info("Redis deleted {} keys with prefix '{}'", deletedCount, prefix);
        }
    }
}
