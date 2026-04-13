package com.nexusfuture.currency.constant;

import java.time.Duration;

public class RedisKeyConstant {
    public static final String CURRENCY_ECB = "currency:ecb";
    /** 日频键过期时间，略长于 24h 以覆盖 ECB 更新间隔 */
    public static final Duration TTL = Duration.ofHours(25);
}
