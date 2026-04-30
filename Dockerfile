# ===================================================================
# Stage 1: 编译阶段 (使用官方存在的 Maven 3.9 + JDK 21 镜像)
# ===================================================================
FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /app

# 复制所有代码
COPY . .

# 自动执行打包，跳过测试
RUN mvn clean package -DskipTests

# ===================================================================
# Stage 2: 运行阶段 (使用轻量级 JRE)
# ===================================================================
FROM eclipse-temurin:21-jre-alpine

# 安装时区数据
RUN apk add --no-cache tzdata && \
    cp /usr/share/zoneinfo/Asia/Shanghai /etc/localtime && \
    echo "Asia/Shanghai" > /etc/timezone

WORKDIR /app

# 从第一阶段复制 JAR 文件
COPY --from=builder /app/target/*.jar app.jar

EXPOSE 8080

# 启动命令
ENTRYPOINT ["java", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-Duser.timezone=Asia/Shanghai", \
    "-Xms128m", "-Xmx256m", \
    "-jar", "app.jar"]
