package xyz.kip.gateway.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

/**
 * 灰度发布管理服务
 *
 * @author xiaoshichuan
 * @version 2026-03-06
 */
@Service
public class GrayReleaseService {

    private static final Logger logger = LoggerFactory.getLogger(GrayReleaseService.class);

    private static final String REDIS_GRAY_CONFIG_PREFIX = "gateway:gray:";

    private final ReactiveRedisTemplate<String, String> redisTemplate;

    public GrayReleaseService(ReactiveRedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * 配置灰度规则
     */
    public Mono<Boolean> configureGrayRelease(String serviceName, GrayReleaseConfig config) {
        String configKey = REDIS_GRAY_CONFIG_PREFIX + serviceName;
        logger.info("Configuring gray release for service: {}, config: {}", serviceName, config);

        Map<String, String> configMap = new HashMap<>();
        configMap.put("enabled", String.valueOf(config.isEnabled()));
        configMap.put("grayVersion", config.getGrayVersion());
        configMap.put("weight", String.valueOf(config.getWeight()));
        if (config.getGrayUserIds() != null) {
            configMap.put("grayUserIds", config.getGrayUserIds());
        }
        if (config.getGrayHeaders() != null) {
            configMap.put("grayHeaders", config.getGrayHeaders());
        }

        return redisTemplate.opsForHash().putAll(configKey, configMap)
                .map(success -> {
                    logger.info("Gray release configured successfully for service: {}", serviceName);
                    return success;
                });
    }

    /**
     * 获取灰度配置
     */
    public Mono<Map<String, String>> getGrayConfig(String serviceName) {
        String configKey = REDIS_GRAY_CONFIG_PREFIX + serviceName;
        return redisTemplate.opsForHash().entries(configKey)
                .collectMap(entry -> entry.getKey().toString(), entry -> entry.getValue().toString());
    }

    /**
     * 删除灰度配置
     */
    public Mono<Boolean> deleteGrayConfig(String serviceName) {
        String configKey = REDIS_GRAY_CONFIG_PREFIX + serviceName;
        logger.info("Deleting gray release config for service: {}", serviceName);
        return redisTemplate.delete(configKey)
                .map(count -> count > 0);
    }

    /**
     * 更新灰度权重
     */
    public Mono<Boolean> updateGrayWeight(String serviceName, int weight) {
        if (weight < 0 || weight > 100) {
            return Mono.error(new IllegalArgumentException("Weight must be between 0 and 100"));
        }

        String configKey = REDIS_GRAY_CONFIG_PREFIX + serviceName;
        logger.info("Updating gray weight for service: {} to {}", serviceName, weight);
        return redisTemplate.opsForHash().put(configKey, "weight", String.valueOf(weight));
    }

    /**
     * 启用/禁用灰度发布
     */
    public Mono<Boolean> toggleGrayRelease(String serviceName, boolean enabled) {
        String configKey = REDIS_GRAY_CONFIG_PREFIX + serviceName;
        logger.info("Toggling gray release for service: {} to {}", serviceName, enabled);
        return redisTemplate.opsForHash().put(configKey, "enabled", String.valueOf(enabled));
    }

    /**
     * 灰度发布配置
     */
    public static class GrayReleaseConfig {
        private boolean enabled;
        private String grayVersion;
        private int weight;
        private String grayUserIds;
        private String grayHeaders;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getGrayVersion() {
            return grayVersion;
        }

        public void setGrayVersion(String grayVersion) {
            this.grayVersion = grayVersion;
        }

        public int getWeight() {
            return weight;
        }

        public void setWeight(int weight) {
            this.weight = weight;
        }

        public String getGrayUserIds() {
            return grayUserIds;
        }

        public void setGrayUserIds(String grayUserIds) {
            this.grayUserIds = grayUserIds;
        }

        public String getGrayHeaders() {
            return grayHeaders;
        }

        public void setGrayHeaders(String grayHeaders) {
            this.grayHeaders = grayHeaders;
        }

        @Override
        public String toString() {
            return "GrayReleaseConfig{" +
                    "enabled=" + enabled +
                    ", grayVersion='" + grayVersion + '\'' +
                    ", weight=" + weight +
                    ", grayUserIds='" + grayUserIds + '\'' +
                    ", grayHeaders='" + grayHeaders + '\'' +
                    '}';
        }
    }
}
