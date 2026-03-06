package xyz.kip;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * Gateway Bootstrap Application
 * 网关启动类，集成Spring Cloud服务发现、网关、限流等功能
 *
 * @author xiaoshichuan
 * @version 2026-02-28
 */
@SpringBootApplication
@EnableDiscoveryClient
public class GatewayBootApplication {
    public static void main(String[] args) {
        SpringApplication.run(GatewayBootApplication.class, args);
    }
}
