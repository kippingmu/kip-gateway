package xyz.kip.gateway.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.event.RefreshRoutesEvent;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.cloud.gateway.route.RouteDefinitionLocator;
import org.springframework.cloud.gateway.route.RouteDefinitionWriter;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import xyz.kip.gateway.model.RouteDefinitionVO;

import java.util.ArrayList;
import java.util.List;

/**
 * 路由管理服务
 *
 * @author xiaoshichuan
 * @version 2026-03-06
 */
@Service
public class RouteManagementService {

    private static final Logger logger = LoggerFactory.getLogger(RouteManagementService.class);

    private final RouteDefinitionLocator routeDefinitionLocator;
    private final RouteDefinitionWriter routeDefinitionWriter;
    private final ApplicationEventPublisher eventPublisher;

    public RouteManagementService(RouteDefinitionLocator routeDefinitionLocator,
                                   RouteDefinitionWriter routeDefinitionWriter,
                                   ApplicationEventPublisher eventPublisher) {
        this.routeDefinitionLocator = routeDefinitionLocator;
        this.routeDefinitionWriter = routeDefinitionWriter;
        this.eventPublisher = eventPublisher;
    }

    /**
     * 获取所有路由
     */
    public Flux<RouteDefinition> getAllRoutes() {
        return routeDefinitionLocator.getRouteDefinitions();
    }

    /**
     * 获取路由列表（转换为 VO）
     */
    public Mono<List<RouteDefinitionVO>> getRouteList() {
        return getAllRoutes()
                .map(this::convertToVO)
                .collectList();
    }

    /**
     * 添加路由
     */
    public Mono<Void> addRoute(RouteDefinition routeDefinition) {
        logger.info("Adding route: {}", routeDefinition.getId());
        return routeDefinitionWriter.save(Mono.just(routeDefinition))
                .doOnSuccess(v -> {
                    logger.info("Route added successfully: {}", routeDefinition.getId());
                    refreshRoutes();
                })
                .doOnError(e -> logger.error("Failed to add route: {}", routeDefinition.getId(), e));
    }

    /**
     * 更新路由
     */
    public Mono<Void> updateRoute(RouteDefinition routeDefinition) {
        logger.info("Updating route: {}", routeDefinition.getId());
        return deleteRoute(routeDefinition.getId())
                .then(addRoute(routeDefinition));
    }

    /**
     * 删除路由
     */
    public Mono<Void> deleteRoute(String routeId) {
        logger.info("Deleting route: {}", routeId);
        return routeDefinitionWriter.delete(Mono.just(routeId))
                .doOnSuccess(v -> {
                    logger.info("Route deleted successfully: {}", routeId);
                    refreshRoutes();
                })
                .doOnError(e -> logger.error("Failed to delete route: {}", routeId, e));
    }

    /**
     * 刷新路由
     */
    public void refreshRoutes() {
        logger.info("Refreshing routes...");
        eventPublisher.publishEvent(new RefreshRoutesEvent(this));
        logger.info("Routes refreshed successfully");
    }

    /**
     * 转换为 VO
     */
    private RouteDefinitionVO convertToVO(RouteDefinition routeDefinition) {
        RouteDefinitionVO vo = new RouteDefinitionVO();
        vo.setId(routeDefinition.getId());
        vo.setUri(routeDefinition.getUri().toString());
        vo.setOrder(routeDefinition.getOrder());
        vo.setMetadata(routeDefinition.getMetadata());

        // 转换 Predicates
        List<RouteDefinitionVO.PredicateDefinition> predicates = new ArrayList<>();
        routeDefinition.getPredicates().forEach(p -> {
            RouteDefinitionVO.PredicateDefinition pd = new RouteDefinitionVO.PredicateDefinition();
            pd.setName(p.getName());
            pd.setArgs(p.getArgs());
            predicates.add(pd);
        });
        vo.setPredicates(predicates);

        // 转换 Filters
        List<RouteDefinitionVO.FilterDefinition> filters = new ArrayList<>();
        routeDefinition.getFilters().forEach(f -> {
            RouteDefinitionVO.FilterDefinition fd = new RouteDefinitionVO.FilterDefinition();
            fd.setName(f.getName());
            fd.setArgs(f.getArgs());
            filters.add(fd);
        });
        vo.setFilters(filters);

        return vo;
    }
}
