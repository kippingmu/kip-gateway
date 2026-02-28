package xyz.kip.gateway.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.cloud.gateway.route.RouteDefinitionRepository;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

/**
 * 动态路由配置加载器
 * 从Nacos配置中心动态加载路由配置
 *
 * @author xiaoshichuan
 * @version 2026-02-28
 */
@Slf4j
@Component
public class DynamicRouteLoader implements RouteDefinitionRepository {

    private final List<RouteDefinition> routes = new ArrayList<>();

    @Override
    public Flux<RouteDefinition> getRouteDefinitions() {
        return Flux.fromIterable(routes);
    }

    @Override
    public Mono<Void> save(Mono<RouteDefinition> route) {
        return route.doOnNext(r -> {
            routes.add(r);
            log.info("Route added: id={}, uri={}", r.getId(), r.getUri());
        }).then();
    }

    @Override
    public Mono<Void> delete(Mono<String> routeId) {
        return routeId.doOnNext(id -> {
            routes.removeIf(r -> r.getId().equals(id));
            log.info("Route deleted: id={}", id);
        }).then();
    }

    /**
     * 更新所有路由
     */
    public void updateRoutes(List<RouteDefinition> newRoutes) {
        routes.clear();
        routes.addAll(newRoutes);
        log.info("Routes updated, total count: {}", routes.size());
    }

    /**
     * 添加单个路由
     */
    public void addRoute(RouteDefinition route) {
        routes.add(route);
        log.info("Route added: id={}, uri={}", route.getId(), route.getUri());
    }

    /**
     * 删除指定ID的路由
     */
    public void removeRoute(String routeId) {
        routes.removeIf(r -> r.getId().equals(routeId));
        log.info("Route removed: id={}", routeId);
    }
}

