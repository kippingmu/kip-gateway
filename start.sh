#!/bin/bash

# 网关启动脚本

echo "=========================================="
echo "  Kip Gateway 启动脚本"
echo "=========================================="
echo ""

# 检查 Java 环境
if ! command -v java &> /dev/null; then
    echo "❌ 错误: 未找到 Java 环境"
    echo "请先安装 JDK 21 或更高版本"
    exit 1
fi

echo "✅ Java 版本:"
java -version
echo ""

# 选择启动模式
echo "请选择启动模式:"
echo "1. 开发环境 (dev) - 使用 Nacos 配置"
echo "2. 本地环境 (local) - 不使用 Nacos，快速启动"
echo "3. 开发环境 (dev) - 禁用 Nacos 配置"
echo ""
read -p "请输入选项 (1/2/3): " choice

case $choice in
    1)
        echo ""
        echo "=========================================="
        echo "  启动模式: 开发环境 (使用 Nacos)"
        echo "=========================================="
        echo ""
        echo "⚠️  注意: 请确保已在 Nacos 中创建配置"
        echo "   Data ID: kip-gateway-dev.yml"
        echo "   Group: DEFAULT_GROUP"
        echo ""
        echo "如果未创建配置，请参考 STARTUP-GUIDE.md"
        echo ""
        read -p "按 Enter 继续启动..."

        java -jar app/gateway-web/target/app.jar \
            --spring.profiles.active=dev
        ;;
    2)
        echo ""
        echo "=========================================="
        echo "  启动模式: 本地环境 (不使用 Nacos)"
        echo "=========================================="
        echo ""
        echo "✅ 快速启动模式，所有配置都在本地"
        echo ""

        java -jar app/gateway-web/target/app.jar \
            --spring.profiles.active=local
        ;;
    3)
        echo ""
        echo "=========================================="
        echo "  启动模式: 开发环境 (禁用 Nacos)"
        echo "=========================================="
        echo ""
        echo "✅ 使用 dev 配置但禁用 Nacos"
        echo ""

        java -jar app/gateway-web/target/app.jar \
            --spring.profiles.active=dev \
            --spring.cloud.nacos.config.enabled=false
        ;;
    *)
        echo "❌ 无效的选项"
        exit 1
        ;;
esac
