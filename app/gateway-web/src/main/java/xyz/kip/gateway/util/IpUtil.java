package xyz.kip.gateway.util;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * IP 地址工具类
 * 支持 CIDR 格式的 IP 段匹配
 *
 * @author xiaoshichuan
 * @version 2026-03-06
 */
public class IpUtil {

    /**
     * 检查 IP 是否匹配 CIDR 规则
     *
     * @param ip   待检查的 IP 地址
     * @param cidr CIDR 格式的 IP 段，如 "192.168.1.0/24"
     * @return 是否匹配
     */
    public static boolean matchesCidr(String ip, String cidr) {
        if (ip == null || cidr == null) {
            return false;
        }

        // 如果不是 CIDR 格式，直接比较
        if (!cidr.contains("/")) {
            return ip.equals(cidr);
        }

        try {
            String[] parts = cidr.split("/");
            String cidrIp = parts[0];
            int prefixLength = Integer.parseInt(parts[1]);

            InetAddress ipAddress = InetAddress.getByName(ip);
            InetAddress cidrAddress = InetAddress.getByName(cidrIp);

            byte[] ipBytes = ipAddress.getAddress();
            byte[] cidrBytes = cidrAddress.getAddress();

            if (ipBytes.length != cidrBytes.length) {
                return false;
            }

            int fullBytes = prefixLength / 8;
            int remainingBits = prefixLength % 8;

            // 比较完整字节
            for (int i = 0; i < fullBytes; i++) {
                if (ipBytes[i] != cidrBytes[i]) {
                    return false;
                }
            }

            // 比较剩余位
            if (remainingBits > 0) {
                int mask = 0xFF << (8 - remainingBits);
                return (ipBytes[fullBytes] & mask) == (cidrBytes[fullBytes] & mask);
            }

            return true;
        } catch (UnknownHostException | NumberFormatException e) {
            return false;
        }
    }

    /**
     * 验证 IP 地址格式是否正确
     */
    public static boolean isValidIp(String ip) {
        if (ip == null || ip.isEmpty()) {
            return false;
        }
        try {
            InetAddress.getByName(ip);
            return true;
        } catch (UnknownHostException e) {
            return false;
        }
    }

    /**
     * 验证 CIDR 格式是否正确
     */
    public static boolean isValidCidr(String cidr) {
        if (cidr == null || cidr.isEmpty()) {
            return false;
        }

        if (!cidr.contains("/")) {
            return isValidIp(cidr);
        }

        try {
            String[] parts = cidr.split("/");
            if (parts.length != 2) {
                return false;
            }

            if (!isValidIp(parts[0])) {
                return false;
            }

            int prefixLength = Integer.parseInt(parts[1]);
            return prefixLength >= 0 && prefixLength <= 32;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
