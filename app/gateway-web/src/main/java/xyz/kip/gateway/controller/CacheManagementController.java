package xyz.kip.gateway.controller;

import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import xyz.kip.gateway.service.CacheManagementService;

import java.util.Map;

/**
 * 缓存管理接口
 *
 * @author xiaoshichuan
 * @version 2026-03-06
 */
@RestController
@RequestMapping("/gateway/cache")
public class CacheManagementController {

    private final CacheManagementService cacheManagementService;

    public CacheManagementController(CacheManagementService cacheManagementService) {
        this.cacheManagementService = cacheManagementService;
    }

    /**
     * 清除指定路径的缓存
     */
    @DeleteMapping("/path")
    public Mono<Map<String, Object>> clearCacheByPath(@RequestParam String path) {
        return cacheManagementService.clearCacheByPath(path)
                .map(count -> Map.of(
                        "success", true,
                        "message", "Cache cleared",
                        "deletedCount", count,
                        "path", path
                ));
    }

    /**
     * 清除所有缓存
     */
    @DeleteMapping("/all")
    public Mono<Map<String, Object>> clearAllCache() {
        return cacheManagementService.clearAllCache()
                .map(count -> Map.of(
                        "success", true,
                        "message", "All cache cleared",
                        "deletedCount", count
                ));
    }

    /**
     * 获取所有缓存键
     */
    @GetMapping("/keys")
    public Flux<String> getAllCacheKeys() {
        return cacheManagementService.getAllCacheKeys();
    }

    /**
     * 获取缓存统计信息
     */
    @GetMapping("/stats")
    public Mono<CacheManagementService.CacheStats> getCacheStats() {
        return cacheManagementService.getCacheStats();
    }
}
