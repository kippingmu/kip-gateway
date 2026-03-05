package xyz.kip.gateway.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import xyz.kip.open.common.dto.ApiResponse;

import java.util.HashMap;
import java.util.Map;

/**
 * 网关管理控制器
 * 提供网关健康检查、路由查询、路由管理等接口
 *
 * @author xiaoshichuan
 * @version 2026-02-28
 */
@RestController
@RequestMapping("/gateway")
public class GatewayManagementController {

    private static final Logger logger = LoggerFactory.getLogger(GatewayManagementController.class);


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
}
