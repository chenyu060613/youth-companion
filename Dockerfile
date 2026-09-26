# ---- 构建阶段：Maven + JDK 17 编译打包 ----
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
# 512MB 免费实例内存有限，限制 Maven 堆内存
ENV MAVEN_OPTS="-Xmx300m"
COPY pom.xml ./
# 先单独下载依赖（利用 Docker 层缓存，改代码时不用重新下载）
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -DskipTests package

# ---- 运行阶段：只带 JRE 17，镜像小、内存占用低 ----
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/youth-companion-0.0.1-SNAPSHOT.jar app.jar
# 内存参数针对 Render 免费实例（512MB）调优
ENV JAVA_OPTS="-Xms64m -Xmx300m -XX:MaxMetaspaceSize=128m -XX:+UseSerialGC"
# 应用通过 server.port=${PORT:8080} 自动读取 Render 注入的 PORT
EXPOSE 8080
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
