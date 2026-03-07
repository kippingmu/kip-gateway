package xyz.kip.gateway.config;

import com.alibaba.csp.sentinel.adapter.gateway.common.SentinelGatewayConstants;
import com.alibaba.csp.sentinel.adapter.gateway.common.api.ApiDefinition;
import com.alibaba.csp.sentinel.adapter.gateway.common.api.ApiPathPredicateItem;
import com.alibaba.csp.sentinel.adapter.gateway.common.api.ApiPredicateItem;
import com.alibaba.csp.sentinel.adapter.gateway.common.api.GatewayApiDefinitionManager;
import com.alibaba.csp.sentinel.adapter.gateway.common.rule.GatewayFlowRule;
import com.alibaba.csp.sentinel.adapter.gateway.common.rule.GatewayRuleManager;
import com.alibaba.csp.sentinel.adapter.gateway.sc.exception.SentinelGatewayBlockExceptionHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.web.reactive.result.view.ViewResolver;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Sentinel 限流降级配置
 * 配置网关限流规则、API 分组、熔断降级处理
 *
 * @author xiaoshichuan
 * @version 2026-03-06
 */
@Configuration
public class SentinelGatewayConfig {

    private static final Logger logger = LoggerFactory.getLogger(SentinelGatewayConfig.class);

    private final List<ViewResolver> viewResolvers;
    private final ServerCodecConfigurer serverCodecConfigurer;

    public SentinelGatewayConfig(ObjectProvider<List<ViewResolver>> viewResolversProvider,
                                  ServerCodecConfigurer serverCodecConfigurer) {
        this.viewResolvers = viewResolversProvider.getIfAvailable(Collections::emptyList);
        this.serverCodecConfigurer = serverCodecConfigurer;
    }

    /**
     * 配置 Sentinel 异常处理器
     */
    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public SentinelGatewayBlockExceptionHandler sentinelGatewayBlockExceptionHandler() {
        return new SentinelGatewayBlockExceptionHandler(viewResolvers, serverCodecConfigurer);
    }


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

        // 规则 2: 基于 API 分组的限流
        GatewayFlowRule rule2 = new GatewayFlowRule("api-group")
                .setResourceMode(SentinelGatewayConstants.RESOURCE_MODE_CUSTOM_API_NAME)
                .setCount(200)
                .setIntervalSec(1);
        rules.add(rule2);

        GatewayRuleManager.loadRules(rules);
        logger.info("Sentinel gateway flow rules initialized, count: {}", rules.size());
        return rules;
    }

    /**
     * 初始化 API 分组
     */
    @Bean
    public Set<ApiDefinition> initApiDefinitions() {
        Set<ApiDefinition> definitions = new HashSet<>();

        // API 分组 1: 认证相关接口
        ApiDefinition authApi = new ApiDefinition("auth-api")
                .setPredicateItems(new HashSet<ApiPredicateItem>() {{
                    add(new ApiPathPredicateItem().setPattern("/api/auth/**")
                            .setMatchStrategy(SentinelGatewayConstants.URL_MATCH_STRATEGY_PREFIX));
                }});
        definitions.add(authApi);

        // API 分组 2: 用户相关接口
        ApiDefinition userApi = new ApiDefinition("user-api")
                .setPredicateItems(new HashSet<ApiPredicateItem>() {{
                    add(new ApiPathPredicateItem().setPattern("/api/user/**")
                            .setMatchStrategy(SentinelGatewayConstants.URL_MATCH_STRATEGY_PREFIX));
                }});
        definitions.add(userApi);

        GatewayApiDefinitionManager.loadApiDefinitions(definitions);
        logger.info("Sentinel API definitions initialized, count: {}", definitions.size());
        return definitions;
    }

}

