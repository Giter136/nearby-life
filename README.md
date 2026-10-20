# Nearby Life（本地生活点评平台）

一个本地生活 / 点评类平台的后端实现：手机号登录、商户浏览与 GEO 附近搜索、达人探店博客、
点赞与关注 Feed 流、用户签到，以及基于 **Redis + Lua + Kafka** 的优惠券秒杀链路。

> 基于黑马程序员本地生活点评课程项目二次开发。各部分的来源与本人工作见下方[归属说明](#项目来源与我的工作)。

---

## 项目来源与我的工作

| 部分 | 来源 | 说明 |
|---|---|---|
| 后端主体<br/>（登录、商户、博客、关注、缓存、GEO、签到、点赞、Feed） | 课程项目 | 跟随本地生活点评课程实现 |
| **秒杀异步下单（Kafka）** | **本人扩展** | 新增 `KafkaConfig` / `KafkaConsumer` / `OrderDTO`，改造 `VoucherOrderServiceImpl`。课程原版不含 Kafka |
| 前端静态页面 | 课程配套资源 | 本人负责接口对接与 nginx 反向代理配置 |
| 接口文档 `docs/api/` | 本人整理 | 5 篇 Markdown |
| 仓库工程化 | 本人 | 目录整合、`.gitignore` 规则、种子数据整理 |

> 说明：本仓库是学习与技术演示用途，非生产系统。

---

## 技术栈

| 层次 | 技术 |
|---|---|
| 后端框架 | Spring Boot 2.3.12.RELEASE（Java 8） |
| 持久层 | MyBatis-Plus 3.4.3 + MySQL（mysql-connector-java 5.1.47） |
| 缓存 | Redis（spring-data-redis 2.6.2 + Lettuce 6.1.6）、Redisson 3.13.6 |
| 消息队列 | Kafka（spring-kafka），topic：`voucher_order` |
| 工具库 | Hutool 5.7.17、Lombok 1.18.30 |
| 前端 | 静态 HTML / CSS / JavaScript，nginx 提供服务并反向代理 `/api` |

**规模**：74 个 Java 文件 / 约 2900 行、9 个 Controller、11 张数据表、2 个 Lua 脚本、10 个前端页面。

---

## 系统架构

```mermaid
flowchart LR
    Browser[浏览器] -->|:8080| Nginx[nginx]
    Nginx -->|静态资源| Page[html/hmdp]
    Nginx -->|/api 反向代理| App[Spring Boot :8081]
    App --> MySQL[(MySQL)]
    App --> Redis[(Redis)]
    App -->|生产| Kafka[(Kafka<br/>voucher_order)]
    Kafka -->|消费| App
```

---

## 核心功能

| 模块 | 能力 | 关键技术 |
|---|---|---|
| 用户 | 手机号验证码登录、token 续期、登出、签到统计 | Redis Hash 存会话、BitMap 签到 |
| 商户 | 详情查询、分类分页、名称搜索、附近商铺 | 缓存穿透 / 击穿 / 逻辑过期、Redis GEO |
| 内容 | 探店博客发布、点赞、点赞排行榜、关注 Feed 流 | Redis ZSet、滚动分页 |
| 社交 | 关注 / 取关、共同关注 | Redis Set 交集 |
| 秒杀 | 优惠券秒杀下单 | Lua 原子预扣、Redis 全局 ID、Kafka 异步落库 |

---

## 秒杀链路设计

```mermaid
sequenceDiagram
    participant C as 客户端
    participant N as nginx
    participant A as Spring Boot
    participant R as Redis
    participant K as Kafka
    participant D as MySQL

    C->>N: POST /api/voucher-order/seckill/{id}
    N->>A: 反向代理至 :8081
    A->>R: EVAL seckill.lua（校验库存 + 一人一单）
    alt 库存不足 / 重复下单
        R-->>A: 返回 1 / 2
        A-->>C: 失败
    else 预扣成功
        R-->>A: 返回 0
        A->>A: RedisIdWorker 生成全局订单号
        A->>K: send(voucher_order, orderDTO)
        A-->>C: 立即返回 orderId（异步处理中）
        K->>A: @KafkaListener 消费
        A->>D: UPDATE stock = stock - 1 WHERE stock > 0
        A->>D: INSERT tb_voucher_order
    end
```

**设计要点**

- **原子性**：库存校验、重复下单校验、扣减库存三步在 `seckill.lua` 中一次完成，避免并发超卖
- **削峰**：请求在 Redis 层快速失败或通过，只有预扣成功的请求才进入 Kafka，数据库不会被打穿
- **全局 ID**：`RedisIdWorker` 用「时间戳 << 32 | 自增序列」生成订单号，避免分库分表下的 ID 冲突
- **异步解耦**：下单接口不等待落库，响应时间与数据库写入解耦

---

## 目录结构

```
nearby-life/
 backend/                     Spring Boot 后端
    pom.xml
    src/main/
        java/com/hmdp/       controller / service / mapper / config / utils
        resources/           application.yaml、MyBatis XML、Lua 脚本、建表 SQL
 frontend/                    nginx 运行环境 + 页面代码
    Dockerfile               前端镜像（nginx + 静态页面）
    conf/nginx.conf          静态资源 + /api 反向代理（本机 nginx）
    html/hmdp/               页面（html / css / js / imgs）
 deploy/nginx/default.conf    容器内 nginx 配置（反代 backend:8081）
 deploy/maven/settings.xml    构建期 Maven 镜像配置（国内构建加速）
 deploy/release.ps1           本机构建并推送镜像到仓库
 deploy/deploy.sh             服务器拉取镜像并重启
 docs/api/                    接口文档（Markdown，5 篇）
 others/                      素材与数据库脚本
 Dockerfile                   后端镜像（Maven 构建 -> JRE 运行）
 docker-compose.yml           一键启动 MySQL / Redis / Kafka / 后端 / nginx
 .env.example                 部署环境变量模板
 README.md
```

---

## 快速开始

### 方式一：Docker Compose（推荐）

一条命令拉起 MySQL、Redis、Kafka、后端与 nginx：

```bash
cp .env.example .env      # 先改掉里面的两个密码
docker compose up -d --build
```

首次启动会自动建库并导入 `backend/src/main/resources/db/hmdp.sql`；
访问 http://localhost （80 端口），`/api` 由 nginx 反代到后端容器。

常用命令：

```bash
docker compose ps                  # 查看状态
docker compose logs -f backend     # 后端日志
docker compose down                # 停止（保留数据卷）
docker compose down -v             # 连数据一起清空，下次启动重新导入种子数据
```

小内存机器（2G 左右）已把 MySQL 缓冲池、Kafka 堆、JVM 堆分别压到 64M / 256M / 256M；
如果再吃紧，可以去掉 `kafka` 服务并停用秒杀链路，其余功能不受影响。

### 方式二：镜像仓库发布（TCR）

服务器上不需要源码、JDK、Maven，只负责拉取和运行。

一次性准备：开通 [TCR 个人版](https://console.cloud.tencent.com/tcr)，设置命名空间与仓库密码，
并创建两个镜像仓库 `hmdp-backend`、`hmdp-web`。

```powershell
# 本机：登录（用户名是腾讯云账号 ID，密码是 TCR 个人版的仓库密码）
docker login ccr.ccs.tencentyun.com --username=1000xxxxxxx

# 构建并推送，带版本号方便回滚
.\deploy\release.ps1 -Registry ccr.ccs.tencentyun.com/<命名空间> -Tag v1.0.0
```

```bash
# 服务器：把 .env 里的 IMAGE_REGISTRY / TAG 改成刚推的版本，然后
cd /opt/hmdp-compose
./deploy/deploy.sh
```

回滚就是把 `TAG` 改回上一个版本，再执行一次 `deploy.sh`——秒级，且不用重新构建。

### 方式三：本地手动启动

**前置**：JDK 8+、Maven、MySQL、Redis、Kafka、nginx

1. **初始化数据库**
   导入 `backend/src/main/resources/db/hmdp.sql`，数据库名 `hmdp`。

2. **启动依赖服务**
   MySQL `3306`、Redis `6379`、Kafka `9092`。

3. **配置本机数据库密码（重要）**

   `application-local.yaml` 不纳入版本控制。新环境先从模板复制：

   ```powershell
   Copy-Item backend\src\main\resources\application-local.yaml.example `
             backend\src\main\resources\application-local.yaml
   ```

   `application.yaml` 默认激活 `local` 环境，可用环境变量 `SPRING_PROFILES_ACTIVE` 覆盖。

4. **启动后端**
   运行 `com.hmdp.HmDianPingApplication`，端口 `8081`。

5. **启动前端**
   在 `frontend/` 目录下启动 nginx（端口 `8080`），访问 http://localhost:8080 。
   页面请求的 `/api` 由 nginx 反向代理到后端 `8081`。

---

## 接口文档

见 [`docs/api/`](docs/api/)。

---

## 注意事项

- **图片上传目录**：默认从当前工作目录逐级向上查找 `frontend/html/hmdp/imgs`，
  因此从 `backend/` 或仓库根目录启动都能正确定位。换机器或部署时可用环境变量
  `HMDP_UPLOAD_DIR`（或配置项 `hmdp.upload.dir`）指定绝对路径。实现见
  `backend/src/main/java/com/hmdp/config/UploadProperties.java`。
- **Docker 数据卷**：`mysql_data`、`kafka_data`、`imgs` 三个命名卷保存运行期数据。
  `imgs` 卷首次创建时由镜像内置的种子图初始化，之后用户上传的图片落在同一卷里，
  重建容器不会丢；要重置请用 `docker compose down -v`。
- **不要提交密码**：`application-local.yaml`、`*.env` 等已在 `.gitignore` 中忽略。
- `frontend/html/hmdp/imgs/blogs/` 下仅有 `hmdp.sql` 引用的 13 张种子图入库，
  运行期上传的图片不入库。

---

## 后续计划

- [ ] 秒杀消费端幂等（唯一索引 + 幂等表）、失败重试与死信队列
- [ ] 补全接口鉴权覆盖，修复图片删除接口的路径校验
- [ ] 引入 Testcontainers 集成测试（秒杀并发、缓存击穿、鉴权）
- [x] Docker Compose 一键启动 MySQL / Redis / Kafka / nginx（见 `docker-compose.yml`）
- [ ] 升级至 Spring Boot 3.x / Java 21
- [ ] 接入 SpringDoc 自动生成接口文档

---

## 致谢

- 课程与前端静态资源：[黑马程序员本地生活点评课程](https://www.itheima.com/)
- 前端页面仓库：[huyi612/hmdp-web](https://gitee.com/huyi612/hmdp-web)
