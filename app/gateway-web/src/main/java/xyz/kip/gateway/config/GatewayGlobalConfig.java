package xyz.kip.gateway.config;

/**
 * GatewayGlobalConfig placeholder.
 * <p>
 * NOTE: original implementation registered Spring beans (KeyResolvers) that caused
 * bean name collisions with framework auto-configuration in this project.
 * <p>
 * To avoid startup conflicts while keeping the source tree clean, this class is
 * intentionally left as a non-configuration placeholder. If you need custom
 * resolvers in the future, reintroduce them with @Bean and use
 * @author xiaoshichuan
 * @ConditionalOnMissingBean to avoid overriding framework beans.
 */
public class GatewayGlobalConfig {
    // Intentionally empty to avoid bean registration collisions.
}
