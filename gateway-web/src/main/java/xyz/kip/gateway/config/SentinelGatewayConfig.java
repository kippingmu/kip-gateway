package xyz.kip.gateway.config;

import com.alibaba.csp.sentinel.adapter.gateway.common.rule.GatewayFlowRule;
import com.alibaba.csp.sentinel.adapter.gateway.common.rule.GatewayRuleManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashSet;
import java.util.Set;

/**
 * Sentinel 限流降级配置（简化）
 * 仅负责初始化 Gateway 的限流规则，避免重复注册 sentinel 过滤器导致启动失败。
 * @author xiaoshichuan
 */
@Configuration
public class SentinelGatewayConfig {

    private static final Logger logger = LoggerFactory.getLogger(SentinelGatewayConfig.class);

    /**
     * 初始化限流规则
     */
    @Bean
    public Set<GatewayFlowRule> initGatewayFlowRules() {
        Set<GatewayFlowRule> rules = new HashSet<>();

        // 规则 1: 基于路由的限流 - auth-service 每秒100个请求
        GatewayFlowRule rule1 = new GatewayFlowRule("auth-service")
                .setCount(100)
                .setIntervalSec(1);
        rules.add(rule1);

        GatewayRuleManager.loadRules(rules);
        logger.info("Sentinel gateway flow rules initialized, count: {}", rules.size());
        return rules;
    }
}
