# ---------------------------------------------------------------------------
# 前端镜像：nginx + 静态页面，/api 反向代理到 backend:8081
# 构建上下文必须是仓库根目录，见 docker-compose.yml
# ---------------------------------------------------------------------------
FROM nginx:1.27-alpine

ENV TZ=Asia/Shanghai

COPY deploy/nginx/default.conf /etc/nginx/conf.d/default.conf
COPY frontend/html/hmdp/ /usr/share/nginx/html/

# imgs 会被 docker-compose 的命名卷挂载覆盖（上传的图片要持久化）。
# 这里的属主决定新卷初始化后的属主，UID 101 与后端镜像的 hmdp 用户对齐。
RUN chown -R 101:0 /usr/share/nginx/html/imgs

EXPOSE 80
