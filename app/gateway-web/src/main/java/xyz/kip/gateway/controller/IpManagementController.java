package xyz.kip.gateway.controller;

import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import xyz.kip.gateway.service.IpManagementService;

import java.util.HashMap;
import java.util.Map;

/**
 * IP 黑白名单管理接口
 *
 * @author xiaoshichuan
 * @version 2026-03-06
 */
@RestController
@RequestMapping("/gateway/ip")
public class IpManagementController {

    private final IpManagementService ipManagementService;

    public IpManagementController(IpManagementService ipManagementService) {
        this.ipManagementService = ipManagementService;
    }

    /**
     * 添加 IP 到黑名单
     */
    @PostMapping("/blacklist")
    public Mono<Map<String, Object>> addToBlacklist(@RequestBody Map<String, String> request) {
        String ip = request.get("ip");
        return ipManagementService.addToBlacklist(ip)
                .map(success -> {
                    Map<String, Object> response = new HashMap<>();
                    response.put("success", success);
                    response.put("message", success ? "IP added to blacklist" : "Failed to add IP");
                    response.put("ip", ip);
                    return response;
                })
                .onErrorResume(e -> Mono.just(Map.of(
                        "success", false,
                        "message", e.getMessage()
                )));
    }

    /**
     * 从黑名单移除 IP
     */
    @DeleteMapping("/blacklist/{ip}")
    public Mono<Map<String, Object>> removeFromBlacklist(@PathVariable String ip) {
        return ipManagementService.removeFromBlacklist(ip)
                .map(success -> Map.of(
                        "success", success,
                        "message", success ? "IP removed from blacklist" : "IP not found in blacklist",
                        "ip", ip
                ));
    }

    /**
     * 获取黑名单列表
     */
    @GetMapping("/blacklist")
    public Flux<String> getBlacklist() {
        return ipManagementService.getBlacklist();
    }

    /**
     * 清空黑名单
     */
    @DeleteMapping("/blacklist")
    public Mono<Map<String, Object>> clearBlacklist() {
        return ipManagementService.clearBlacklist()
                .map(success -> Map.of(
                        "success", success,
                        "message", "Blacklist cleared"
                ));
    }

    /**
     * 添加 IP 到白名单
     */
    @PostMapping("/whitelist")
    public Mono<Map<String, Object>> addToWhitelist(@RequestBody Map<String, String> request) {
        String ip = request.get("ip");
        return ipManagementService.addToWhitelist(ip)
                .map(success -> {
                    Map<String, Object> response = new HashMap<>();
                    response.put("success", success);
                    response.put("message", success ? "IP added to whitelist" : "Failed to add IP");
                    response.put("ip", ip);
                    return response;
                })
                .onErrorResume(e -> Mono.just(Map.of(
                        "success", false,
                        "message", e.getMessage()
                )));
    }

    /**
     * 从白名单移除 IP
     */
    @DeleteMapping("/whitelist/{ip}")
    public Mono<Map<String, Object>> removeFromWhitelist(@PathVariable String ip) {
        return ipManagementService.removeFromWhitelist(ip)
                .map(success -> Map.of(
                        "success", success,
                        "message", success ? "IP removed from whitelist" : "IP not found in whitelist",
                        "ip", ip
                ));
    }

    /**
     * 获取白名单列表
     */
    @GetMapping("/whitelist")
    public Flux<String> getWhitelist() {
        return ipManagementService.getWhitelist();
    }

    /**
     * 清空白名单
     */
    @DeleteMapping("/whitelist")
    public Mono<Map<String, Object>> clearWhitelist() {
        return ipManagementService.clearWhitelist()
                .map(success -> Map.of(
                        "success", success,
                        "message", "Whitelist cleared"
                ));
    }
}
