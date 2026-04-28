# 使用官方的 Eclipse Temurin Java 21 运行时（轻量且兼容性好）
FROM eclipse-temurin:21-jre-alpine

# 设置时区为上海（解决日志时间差 8 小时的问题）
RUN apk add --no-cache tzdata && \
    cp /usr/share/zoneinfo/Asia/Shanghai /etc/localtime && \
    echo "Asia/Shanghai" > /etc/timezone

# 设置工作目录
WORKDIR /app

# 复制打包好的 JAR 文件
COPY target/currency-service-0.0.1-SNAPSHOT.jar app.jar

# 🔥 新增：将远程配置文件也复制进镜像
COPY src/main/resources/application.properties /app/application.properties

# 暴露端口
EXPOSE 8080

# 启动命令：显式指定加载外部配置文件
ENTRYPOINT ["java", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-Duser.timezone=Asia/Shanghai", \
    "-Xms512m", "-Xmx512m", \
    "-jar", "app.jar", \
    "--spring.config.location=file:/app/application.properties", \
    "--server.port=8080"]
