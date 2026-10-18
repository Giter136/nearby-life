# 近邻生活 · 业务与权限模型

> 本文以**源码为准**，对照 `docs/api/` 的记录。两者冲突时，以 `backend/src/main/java/` 的实际行为为准。  
> 目的：说明这个项目在做什么、哪些页面游客能看、哪些操作必须登录，以及前端应该如何处理 401。

---

## 1. 项目是什么

杭州本地生活点评平台：

- **找店**：按类别浏览商铺，按距离查看附近商铺，看商户详情、营业信息与代金券。
- **看内容**：热门探店笔记、笔记详情、作者主页。
- **社交**：登录后点赞、关注、查看关注动态、共同关注。
- **交易**：登录后抢秒杀券；下单走 Redis Lua 原子预扣 + Kafka 异步落库。
- **个人**：手机号验证码登录、我的笔记、资料档案、发布探店笔记。

没有后台管理界面；没有商家端；没有真实支付；短信验证码只写日志。

---

## 2. 账号与登录

| 项 | 事实 |
|---|---|
| 登录方式 | 手机号 + 验证码：`POST /user/code` → `POST /user/login` |
| 新用户 | 验证码通过后按手机号自动注册，昵称为 `用户` + 手机号脱敏 |
| 会话 | 后端生成 UUID token，存 Redis `login:token:{token}`，TTL 1 小时（`RedisConstants.LOGIN_USER_TTL = 3600` 秒；文档写的 10 小时与代码不符） |
| 前端 | token 存 `sessionStorage.token`，之后请求带 `authorization` 头 |
| 密码登录 | **`UserServiceImpl.login` 目前只校验验证码**，传入 `password` 也会走验证码分支；`login2.html` 的密码登录当前无法成功 |
| 登出 | `POST /user/logout` 后端返回“功能未完成”；前端按本地会话处理，清 token 后回首页 |
| 用户资料 | `tb_user_info` 种子数据为空；接口对没有详情行的用户返回 `data: null` |

### 登录拦截器的真实放行范围

`backend/.../config/MvcConfig.java` 注册了 `LoginInterceptor`，只放行：

```
POST /user/code
POST /user/login
GET  /blog/hot
     /shop/**          （商铺详情、按类型、按名称搜索）
     /voucher/list/**  （店铺券列表）
POST /voucher/seckill  （后台新增秒杀券接口，前端不使用）
```

**其余所有接口在未登录时返回 HTTP 401**，包括：

- `GET /shop-type/list`（文档写的是“无需登录”，实际被拦截）
- `GET /blog/{id}`（笔记详情）
- `GET /user/info/{id}`、`GET /user/{id}`
- `GET /blog/of/user`、`GET /blog/likes/{id}`
- `GET /follow/**`、`GET /blog/of/me`、`GET /blog/of/follow`
- `POST /blog`、`POST /upload/blog`、`POST /voucher-order/seckill/{id}`

> 注意：`/shop/**` 是整段放行，连 `POST /shop`、`PUT /shop`（文档要求登录）也会被放行；
> `/voucher/seckill` 是后台“新增秒杀券”接口，也被放行。前端没有调用这两个写接口。

> 结论：**当前后端真正“完全不用登录”的页面只有首页的笔记流、商户列表、商户详情、登录页**。  
> 笔记详情、作者主页的业务文档标注为公开，但实际会被 401 拦住。前端不能绕过这一点，只能在页面上给出登录入口。

---

## 3. 页面访问矩阵

| 页面 | 业务定位 | 后端真实情况 | 前端策略 |
|---|---|---|---|
| `index.html` 首页 | 公开 | `/blog/hot` 公开；`/shop-type/list` 401 | 游客正常看热门笔记；城市分类栏提示登录，不请求分类 |
| `shop-list.html` 商户列表 | 公开 | `/shop/of/type` 公开；分类列表 401 | 游客按 URL 中类别正常看商户；切换类别需要登录 |
| `shop-detail.html` 商户详情 | 公开 | `/shop/{id}`、`/voucher/list/{id}` 公开 | 完全公开；抢券时才要求登录 |
| `blog-detail.html` 笔记详情 | 文档标公开，实际 401 | `/blog/{id}` 受拦截 | 先请求；401 时页面内显示“登录后查看”，**不再全局跳转** |
| `other-info.html` 作者主页 | 文档标公开，实际 401 | `/user/{id}` 等受拦截 | 先请求；401 时页面内显示“登录后查看” |
| `info.html` 我的主页 | 私有 | `/user/me` 受拦截 | 未登录直接去登录页，登录后回本页 |
| `info-edit.html` 资料档案 | 私有 | `/user/me`、`/user/info` 受拦截 | 同上 |
| `blog-edit.html` 写笔记 | 私有 | `/user/me`、上传、发笔记受拦截 | 同上 |
| `login.html` / `login2.html` | 公开 | 放行 | 登录后回到 `redirect` 参数指定的页面 |

### 前端据此采用的规则

1. **浏览页不因为一次辅助请求的 401 而整页跳登录。** `api.js` 不再全局跳登录页，401 只作为 `err.auth` 抛给页面。
2. **游客不主动调用已知需要登录的辅助接口**：
   - 首页、商户列表不请求 `/shop-type/list`；
   - 笔记详情不请求点赞列表、关注状态、`/user/me`；
   - 作者主页不请求关注状态、共同关注；
   - 我的主页、资料档案、写笔记直接走登录拦截。
3. **收藏/点赞/关注/抢券/发布/上传** 都先判断本地 token；未登录时提示并带 `redirect` 去登录。
4. 登录页登录成功后回到原页面（`/login.html?redirect=...`），不再是固定回首页。
5. 登录过期（有 token 但接口 401）时清除本地会话：
   - 浏览页按游客继续显示可公开的数据；
   - 私有页回到登录页；
   - 操作类提示“登录已过期”。

---

## 4. 接口权限与业务规则

### 4.1 公开（拦截器放行）

| 接口 | 业务 |
|---|---|
| `POST /user/code?phone=` | 生成 6 位验证码，存 Redis 2 分钟，只写日志不真发短信 |
| `POST /user/login` | 验证码登录、自动注册、返回 token |
| `GET /blog/hot?current=` | 按点赞数倒序的热门笔记，分页 10；填充作者昵称/头像；登录时附带 `isLike` |
| `GET /shop/{id}` | 商户详情；逻辑过期缓存 + 互斥重建；不计算距离 |
| `GET /shop/of/type` | 按类别分页；带 `x/y` 时走 Redis GEO，5km 内按距离排序 |
| `GET /shop/of/name` | 名称模糊搜索；发布笔记选店用 |
| `GET /voucher/list/{shopId}` | 店铺券列表；联表带出秒杀库存与起止时间 |

### 4.2 需要登录

| 接口 | 业务规则 |
|---|---|
| `GET /user/me` | 当前登录用户（id/nickName/icon） |
| `GET /blog/{id}` | 笔记详情；填充作者与 `isLike` |
| `GET /blog/likes/{id}` | 返回 Redis ZSet 中前 5 个点赞用户 |
| `GET /blog/of/me` | 当前用户笔记，分页 10；**不填充作者信息** |
| `GET /blog/of/user?id=&current=` | 某用户笔记，分页 10；**不填充作者信息** |
| `GET /blog/of/follow` | 关注 Feed；实际映射为 `/blog/blog/of/follow`，按文档路径会 404 |
| `PUT /blog/like/{id}` | 点赞/取消点赞；按 Redis 是否已点赞切换 |
| `PUT /follow/{id}/{isFollow}` | 关注/取关；同时维护 Redis `follow:{userId}` 集合 |
| `GET /follow/or/not/{id}` | 是否关注 |
| `GET /follow/common/{id}` | Service 有实现，**Controller 未暴露路由**，实际 404 |
| `POST /blog` | 发布笔记；保存后写粉丝 Feed |
| `POST /upload/blog` / `GET /upload/blog/delete` | 图片上传/删除；删除兼容 `/imgs/...` 与 `/blogs/...` |
| `POST /voucher-order/seckill/{id}` | 秒杀；Lua 原子校验库存与一人一单，Kafka 异步落库，立即返回订单号 |

---

## 5. 数据规则（容易理解错的地方）

| 字段 | 规则 |
|---|---|
| 金额 `payValue` / `actualValue` | 单位是**分**；前端 `/100` 显示元 |
| `Shop.avgPrice` | 单位是**元**，整数 |
| `Shop.score` | 评分 ×10 保存；37 表示 3.7 分，展示时要 `/10` |
| `Shop.sold` | **销量**，不是收藏数 |
| `Shop.comments` | 评论数量（当前没有评论明细接口） |
| `Shop.distance` | 仅 `/shop/of/type` 带 `x/y` 时由 GEO 计算，详情接口没有 |
| `Blog.images` | 逗号分隔，前端取第一张做封面 |
| `Blog.isLike` | 仅登录时由后端填充；游客没有 |
| `Blog.content` | HTML 字符串，详情页按富文本渲染 |
| `UserInfo.gender` | DB：0 男、1 女；接口序列化为 Boolean（`false` 男 / `true` 女） |
| `UserInfo.level` | DB 是 0~9 的会员等级，但实体映射为 `Boolean`，接口只能表达“是否开通” |
| `UserInfo.fans/followee` | 字段存在，但关注接口**没有同步更新这两个计数**，展示的是存量值 |
| 图片路径 | 上传返回 `/blogs/a/b/uuid.jpg`；前端拼 `/imgs` 访问；DB 里也存 `/imgs/...` |
| 店铺分类图标 | `icon` 形如 `/types/ms.png`，前端拼 `/imgs` |

---

## 6. 核心流程

### 6.1 游客浏览

1. 首页：`/blog/hot` 拉热门笔记 → 点击笔记进入详情。
2. 商户：`/shop/of/type`（带当前坐标）→ 商户详情 → 券列表。
3. 游客点“抢购/点赞/关注”时才提示登录。

### 6.2 登录

1. 输入手机号 → 发送验证码（Redis 2 分钟）。
2. 输入验证码 → `/user/login`，自动注册，返回 token。
3. token 存 `sessionStorage`，后续请求带 `authorization`。
4. 从受保护页面被引导来的，登录后回到原页面。

### 6.3 点赞

1. `PUT /blog/like/{id}`：Redis 中存在则取消，不存在则点赞。
2. 重新 `GET /blog/{id}` 取最新数量与状态。
3. 点赞头像来自 `GET /blog/likes/{id}`（登录才可见，最多 5 个）。

### 6.4 关注与 Feed

- 关注写 `tb_follow` 与 Redis `follow:{userId}`。
- 粉丝收件箱 `feed:{fanId}` 在发布笔记时写入。
- Feed 查询按 score 倒序游标分页。
- **注意**：Controller 实际路径是 `/blog/blog/of/follow`，文档和前端用的是 `/blog/of/follow`，当前会 404。
- 共同关注 Service 已实现，但 Controller 没暴露。

### 6.5 发布笔记

1. 上传图片 → 返回 `/blogs/...`，前端展示为 `/imgs/blogs/...`。
2. 选择商户（`/shop/of/name`）。
3. `POST /blog`，body：`title/content/images/shopId`。
4. 后端保存后，把笔记 ID 推给粉丝收件箱。

### 6.6 秒杀券

1. `GET /voucher/list/{shopId}` 取券、库存、起止时间。
2. 前端只展示未结束的券，并区分未开始/已抢光。
3. 点击抢购 → `POST /voucher-order/seckill/{id}`。
4. 后端 Lua 原子校验库存与一人一单，返回 orderId；Kafka 消费者异步扣库与落单。

---

## 7. 已实现 / 未实现（以代码为准）

| 能力 | 状态 |
|---|---|
| 验证码登录、自动注册、token 续期 | 已实现 |
| 密码登录 | **未实现**（Service 不读 password） |
| 登出 | 接口返回“功能未完成”；前端本地清 token |
| 热门笔记、笔记详情、我的笔记、某用户笔记 | 已实现 |
| 关注动态 Feed | 查询逻辑已实现，但 Controller 路径错位；前端调用会 404 |
| 共同关注 | Service 已实现，Controller 未暴露 |
| 点赞/取消点赞、点赞列表 | 已实现 |
| 评论列表/发表/删除 | **未实现**（Controller 为空） |
| 我的资料读取 | 已实现；`tb_user_info` 默认空 |
| 资料修改 | **未实现**（无更新接口） |
| 图片上传/删除 | 已实现 |
| 商铺详情/搜索/优惠券 | 已实现 |
| 商铺按类型列表 | **Controller 接错方法**：`ShopController#queryShopByType` 最后调用 `shopService.queryShopByName(typeId,current,x,y)`，而该实现直接 `return null`，所以 `GET /shop/of/type` 返回 200 空 body；应改为 `queryShopByType(...)` |
| GEO 距离 | 逻辑已实现；需要先运行 `HmDianPingApplicationTests#test3` 把 `shop:geo:{typeId}` 写入 Redis，否则按距离分支返回空列表 |
| 优惠券列表/秒杀下单 | 已实现（Lua + Kafka） |
| 按人气/评分排序 | **未实现**：Controller 不接收 `sortBy`，前端只能对已加载页排序 |

---

## 8. 要让“浏览类页面全部免登录”需要后端改什么

前端已经做到：不全局跳转、游客不请求已知受限的辅助接口、页面内给登录入口。  
要真正让 **笔记详情、作者主页、分类列表** 公开，只需后端把对应路径加入 `MvcConfig` 的排除列表（不由本次前端改动完成）：

```java
.excludePathPatterns(
    "/user/code", "/user/login",
    "/blog/hot", "/blog/*", "/blog/of/user",
    "/shop/**", "/shop-type/list",
    "/voucher/list/**",
    "/user/info/**", "/user/*"
)
```

同时建议修复：

- `/blog/of/follow` 控制器映射（去掉重复的 `/blog`）。
- 暴露 `GET /follow/common/{userId}`。
- 密码登录分支按 `LoginFormDTO.password` 实现。
- 关注时同步 `fans/followee` 计数。
- 评论接口（如果 UI 要评论区）。
- `/shop/of/type` 接收 `sortBy` 或移除前端排序入口。
- 修复 `ShopController#queryShopByType` 调错方法的问题（改为 `queryShopByType`）。

> 本次改动**没有修改任何后端文件**；以上仅是前端在现有接口下的行为边界。
