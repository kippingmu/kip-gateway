package xyz.kip.gateway.config;

import com.alibaba.csp.sentinel.adapter.gateway.common.rule.GatewayFlowRule;
import com.alibaba.csp.sentinel.adapter.gateway.common.rule.GatewayRuleManager;
import com.alibaba.csp.sentinel.adapter.gateway.sc.SentinelGatewayFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

import java.util.HashSet;
import java.util.Set;

/**
 * Sentinel 限流降级配置
 * 集成Sentinel进行流量控制和熔断降级
 *
 * @author xiaoshichuan
 * @version 2026-02-28
 */
@Configuration
public class SentinelGatewayConfig {

    private static final Logger logger = LoggerFactory.getLogger(SentinelGatewayConfig.class);

    /**
     * 初始化Sentinel网关过滤器
     */
    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public GlobalFilter sentinelGatewayFilter() {
        return new SentinelGatewayFilter();
    }

    /**
     * 初始化限流规则
     */
    @Bean
    public void initGatewayFlowRules() {
        Set<GatewayFlowRule> rules = new HashSet<>();

        // 规则 1: 基于路由的限流 - user-service 每秒100个请求
        GatewayFlowRule rule1 = new GatewayFlowRule("user-service")
                .setCount(100)
                .setIntervalSec(1);
        rules.add(rule1);

        // 规则 2: 基于路由的限流 - order-service 每秒50个请求
        GatewayFlowRule rule2 = new GatewayFlowRule("order-service")
                .setCount(50)
                .setIntervalSec(1);
        rules.add(rule2);

        // 规则 3: 基于路由的限流 - product-service 每秒200个请求
        GatewayFlowRule rule3 = new GatewayFlowRule("product-service")
                .setCount(200)
                .setIntervalSec(1);
        rules.add(rule3);

        GatewayRuleManager.loadRules(rules);
        logger.info("Sentinel gateway flow rules initialized, count: {}", rules.size());
    }
}
