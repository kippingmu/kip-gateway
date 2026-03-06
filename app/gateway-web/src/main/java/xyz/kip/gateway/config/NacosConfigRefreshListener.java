package xyz.kip.gateway.config;

import com.alibaba.nacos.api.config.annotation.NacosConfigListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import xyz.kip.gateway.service.RouteManagementService;

/**
 * Nacos 配置热更新监听器
 *
 * @author xiaoshichuan
 * @version 2026-03-06
 */
@Component
@RefreshScope
public class NacosConfigRefreshListener {

    private static final Logger logger = LoggerFactory.getLogger(NacosConfigRefreshListener.class);

    private final RouteManagementService routeManagementService;

    public NacosConfigRefreshListener(RouteManagementService routeManagementService) {
        this.routeManagementService = routeManagementService;
    }

    /**
     * 监听路由配置变化
     * 当 Nacos 中的路由配置发生变化时，自动刷新路由
     */
    @NacosConfigListener(dataId = "${spring.application.name}.yml", groupId = "DEFAULT_GROUP")
    public void onRouteConfigChange(String configInfo) {
        logger.info("Route configuration changed, refreshing routes...");
        try {
            routeManagementService.refreshRoutes();
            logger.info("Routes refreshed successfully after config change");
        } catch (Exception e) {
            logger.error("Failed to refresh routes after config change", e);
        }
    }
}
