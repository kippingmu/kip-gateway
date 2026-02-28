package xyz.kip.gateway.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.cloud.gateway.route.RouteDefinitionLocator;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import xyz.kip.gateway.config.DynamicRouteLoader;
import xyz.kip.gateway.dto.ApiResponse;

import java.util.HashMap;
import java.util.Map;

/**
 * 网关管理控制器
 * 提供网关健康检查、路由查询、路由管理等接口
 *
 * @author xiaoshichuan
 * @version 2026-02-28
 */
@Slf4j
@RestController
@RequestMapping("/gateway")
@RequiredArgsConstructor
public class GatewayManagementController {

    private final RouteDefinitionLocator routeDefinitionLocator;
    private final DynamicRouteLoader dynamicRouteLoader;

    /**
     * 健康检查接口
     */
    @GetMapping("/health")
    public ResponseEntity<ApiResponse<Map<String, Object>>> health() {
        Map<String, Object> data = new HashMap<>();
        data.put("status", "UP");
        data.put("timestamp", System.currentTimeMillis());
        data.put("version", "1.0.0");
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    /**
     * 获取所有路由信息
     */
    @GetMapping("/routes")
    public ResponseEntity<Map<String, Object>> getAllRoutes() {
        try {
            Flux<RouteDefinition> routes = routeDefinitionLocator.getRouteDefinitions();
            routes.subscribe(route ->
                    log.debug("Route: id={}, uri={}, predicates={}",
                            route.getId(), route.getUri(), route.getPredicates())
            );

            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("message", "success");
            response.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to get routes: {}", e.getMessage(), e);
            Map<String, Object> error = new HashMap<>();
            error.put("code", 500);
            error.put("message", "Failed to get routes");
            error.put("error", e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }

    /**
     * 获取特定路由信息
     */
    @GetMapping("/routes/{routeId}")
    public ResponseEntity<Map<String, Object>> getRoute(@PathVariable String routeId) {
        try {
            Flux<RouteDefinition> routes = routeDefinitionLocator.getRouteDefinitions();
            Map<String, Object> response = new HashMap<>();

            routes.filter(route -> route.getId().equals(routeId))
                    .subscribe(route -> {
                        response.put("code", 200);
                        response.put("message", "success");
                        response.put("data", route);
                        response.put("timestamp", System.currentTimeMillis());
                    });

            if (!response.containsKey("code")) {
                response.put("code", 404);
                response.put("message", "Route not found");
                response.put("timestamp", System.currentTimeMillis());
                return ResponseEntity.status(404).body(response);
            }

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to get route {}: {}", routeId, e.getMessage(), e);
            Map<String, Object> error = new HashMap<>();
            error.put("code", 500);
            error.put("message", "Failed to get route");
            error.put("error", e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }

    /**
     * 添加新路由
     */
    @PostMapping("/routes")
    public ResponseEntity<Map<String, Object>> addRoute(@RequestBody RouteDefinition routeDefinition) {
        try {
            dynamicRouteLoader.addRoute(routeDefinition);

            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("message", "Route added successfully");
            response.put("routeId", routeDefinition.getId());
            response.put("timestamp", System.currentTimeMillis());

            log.info("Route added: id={}", routeDefinition.getId());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to add route: {}", e.getMessage(), e);
            Map<String, Object> error = new HashMap<>();
            error.put("code", 500);
            error.put("message", "Failed to add route");
            error.put("error", e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }

    /**
     * 删除路由
     */
    @DeleteMapping("/routes/{routeId}")
    public ResponseEntity<Map<String, Object>> deleteRoute(@PathVariable String routeId) {
        try {
            dynamicRouteLoader.removeRoute(routeId);

            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("message", "Route deleted successfully");
            response.put("routeId", routeId);
            response.put("timestamp", System.currentTimeMillis());

            log.info("Route deleted: id={}", routeId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to delete route {}: {}", routeId, e.getMessage(), e);
            Map<String, Object> error = new HashMap<>();
            error.put("code", 500);
            error.put("message", "Failed to delete route");
            error.put("error", e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }

    /**
     * 获取网关统计信息
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        try {
            Map<String, Object> stats = new HashMap<>();
            stats.put("uptime", System.currentTimeMillis());
            stats.put("totalMemory", Runtime.getRuntime().totalMemory());
            stats.put("freeMemory", Runtime.getRuntime().freeMemory());
            stats.put("processorCount", Runtime.getRuntime().availableProcessors());

            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("message", "success");
            response.put("data", stats);
            response.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to get stats: {}", e.getMessage(), e);
            Map<String, Object> error = new HashMap<>();
            error.put("code", 500);
            error.put("message", "Failed to get stats");
            error.put("error", e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }
}

