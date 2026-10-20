# ---------------------------------------------------------------------------
# 后端镜像：多阶段构建（Maven 编译 -> JRE 运行）
# 构建上下文必须是仓库根目录，见 docker-compose.yml
# ---------------------------------------------------------------------------

# ---------- 构建阶段 ----------
FROM maven:3.9-eclipse-temurin-8 AS build
WORKDIR /build

# 依赖走腾讯云镜像，国内构建会快一个数量级（见 deploy/maven/settings.xml）
COPY deploy/maven/settings.xml /root/.m2/settings.xml

# 先只拷 pom，利用 Docker 层缓存：依赖不变时不会重复下载
COPY backend/pom.xml ./pom.xml
RUN mvn -B -q dependency:go-offline

COPY backend/src ./src
RUN mvn -B -q -DskipTests package \
    && cp target/hm-dianping-*.jar /build/app.jar

# ---------- 运行阶段 ----------
FROM eclipse-temurin:8-jre-jammy

ENV TZ=Asia/Shanghai \
    LANG=C.UTF-8

# UID 101 与前端 nginx 镜像中 imgs 目录的属主保持一致，
# 这样 backend 与 nginx 共享上传卷时都能读写（详见 docker-compose.yml 的 imgs 卷）
RUN useradd -u 101 -g 0 -M -s /usr/sbin/nologin hmdp \
    && mkdir -p /app /data/imgs \
    && chown -R 101:0 /app /data

WORKDIR /app
COPY --from=build /build/app.jar /app/app.jar

ENV JAVA_OPTS="-Xms128m -Xmx256m -XX:MaxMetaspaceSize=160m -XX:+UseSerialGC -Xss512k -Dfile.encoding=UTF-8"

USER hmdp
EXPOSE 8081

# JAVA_OPTS 用变量形式展开，便于在 compose 里按内存调整
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
