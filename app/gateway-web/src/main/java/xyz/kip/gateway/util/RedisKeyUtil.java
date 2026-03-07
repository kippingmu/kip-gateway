package xyz.kip.gateway.util;

/**
 * Redis key generator utility.
 * Provides consistent key naming with prefix "kip:auth" for all cache keys.
 *
 * @author xiaoshichuan
 */
public class RedisKeyUtil {

    private static final String SEPARATOR = ":";
    private static final String PREFIX = "kip:auth";

    /**
     * Private constructor to prevent instantiation.
     */
    private RedisKeyUtil() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Generate Redis key with prefix.
     * Builds key in format: "kip:auth:part1:part2:..."
     *
     * @param parts key parts to join
     * @return formatted Redis key
     */
    public static String buildKey(String... parts) {
        StringBuilder key = new StringBuilder(PREFIX);
        for (String part : parts) {
            if (part != null && !part.isEmpty()) {
                key.append(SEPARATOR).append(part);
            }
        }
        return key.toString();
    }

    /**
     * Generate user token key.
     * Format: "kip:auth:user:token:{userId}"
     *
     * @param userId user ID
     * @return user token cache key
     */
    public static String userTokenKey(String userId) {
        return buildKey("user", "token", userId);
    }

    /**
     * Generate user info key.
     * Format: "kip:auth:user:info:{userId}"
     *
     * @param userId user ID
     * @return user info cache key
     */
    public static String userInfoKey(String userId) {
        return buildKey("user", "info", userId);
    }

    /**
     * Generate biz info key.
     * Format: "kip:auth:biz:info:{bizCode}"
     *
     * @param bizCode business code
     * @return biz info cache key
     */
    public static String bizInfoKey(String bizCode) {
        return buildKey("biz", "info", bizCode);
    }

    /**
     * Generate session key.
     * Format: "kip:auth:session:{sessionId}"
     *
     * @param sessionId session ID
     * @return session cache key
     */
    public static String sessionKey(String sessionId) {
        return buildKey("session", sessionId);
    }

    /**
     * Generate verification code key.
     * Format: "kip:auth:verify:code:{phone}"
     *
     * @param phone phone number
     * @return verification code cache key
     */
    public static String verifyCodeKey(String phone) {
        return buildKey("verify", "code", phone);
    }

    /**
     * Generate rate limit key.
     * Format: "kip:auth:rate:limit:{identifier}"
     *
     * @param identifier rate limit identifier (e.g., IP, user ID, API endpoint)
     * @return rate limit cache key
     */
    public static String rateLimitKey(String identifier) {
        return buildKey("rate", "limit", identifier);
    }

    /**
     * Generate lock key.
     * Format: "kip:auth:lock:{resource}"
     *
     * @param resource resource to lock
     * @return distributed lock cache key
     */
    public static String lockKey(String resource) {
        return buildKey("lock", resource);
    }
}
