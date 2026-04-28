# ===================================================================
# 第一阶段：编译构建（使用包含 Maven 的 JDK 镜像）
# ===================================================================
FROM maven:3.8.6-eclipse-temurin-21 AS builder
WORKDIR /app
# 复制所有代码
COPY . .
# 全自动执行打包，跳过测试
RUN mvn clean package -DskipTests

# ===================================================================
# 第二阶段：运行环境（使用轻量级 JRE 镜像）
# ===================================================================
FROM eclipse-temurin:21-jre-alpine

# 1. 安装时区数据（解决日志时间差 8 小时问题）
RUN apk add --no-cache tzdata && \
    cp /usr/share/zoneinfo/Asia/Shanghai /etc/localtime && \
    echo "Asia/Shanghai" > /etc/timezone

WORKDIR /app

# 2. 从第一阶段复制打包好的 JAR（application.properties 已经在 JAR 里了）
COPY --from=builder /app/target/*.jar app.jar

EXPOSE 8080

# 3. 启动命令：增加内存限制和时区参数
ENTRYPOINT ["java", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-Duser.timezone=Asia/Shanghai", \
    "-Xms512m", "-Xmx512m", \
    "-jar", "app.jar"]
