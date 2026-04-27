# 使用官方的 Eclipse Temurin Java 21 运行时（轻量且兼容性好）
FROM eclipse-temurin:21-jre-alpine

# 设置工作目录
WORKDIR /app

# 复制打包好的 JAR 文件（需要在宿主机先执行 mvn package）
COPY target/currency-service-0.0.1-SNAPSHOT.jar app.jar

# 暴露端口（默认 8080）
EXPOSE 8080

# 启动命令（改为读取 application.properties，不再硬编码数据库连接）
ENTRYPOINT ["java", "-jar", "app.jar", "--server.port=8080"]
