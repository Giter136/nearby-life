# 前端桌面端重写说明

> 分支：`feat/desktop-redesign`  
> 范围：`frontend/html/hmdp/` 全部页面与静态资源（nginx 配置与部署路径不变）

---

## 1. 目标与约束

- 把原先按手机屏幕设计的 10 个静态页面，重写为**桌面端优先**的城市生活点评界面。
- **接口严格一一对应**：不新增、不改名、不合并任何后端接口；页面里每个请求路径、方法、
  查询参数都保持原有调用（仅按登录态决定“是否发起”，细节见第 4 节）。
- **浏览优先**：业务上应公开的浏览页（首页、商户列表、商户详情）游客可看；只有
  我的主页、资料档案、写笔记等私有页面以及点赞/关注/抢券/发布等写操作才要求登录。
  当前后端拦截器的真实放行范围与文档不一致，前端按“页面内提示 + 登录后回原页”处理，
  详见 [business-model.md](business-model.md)。
- 保持原有页面地址与跳转关系（`/index.html`、`/shop-list.html?type=&name=` …），
  nginx 的 `root html/hmdp` 与 `/api` 反向代理无需修改。
- 技术栈仍为静态 HTML + Vue 2 + axios，**移除 Element UI**（约 870 KB 的 css/js/字体），
  改为自建轻量组件与设计系统。

## 2. 视觉方向

**「城市生活志 / 编辑部排版」**：一份关于杭州的地方生活周报。

| 维度 | 选择 |
|---|---|
| 用户与任务 | 20–35 岁城市用户；逛笔记、按类别找店、抢券、写笔记、管理主页 |
| 目标感受 | 可信、有温度、有城市感；像翻阅一份排版讲究的纸媒，而不是通用 App 模板 |
| 核心隐喻 | 报纸版式 + 城市索引编号 + 票根式优惠券 |
| 构图 | 12 栏纸面网格；主栏 + 编辑部侧栏；细发丝线分隔；编号 01–10；8/4 不对称 |
| 色彩 | 纸白 `#f2ede4`、墨黑 `#17130e`、朱红 `#bf3b1f`、青铜 `#8a7049`、苔绿 `#4b5a43` |
| 字体 | 标题/正文宋体（Songti SC / SimSun），界面无衬线（PingFang / 雅黑），数字 Bahnschrift / Consolas |
| 质感 | 极轻的 SVG 纸纹、1px 墨线、无圆角、无投影卡片；仅弹层与下拉使用硬边投影 |
| 图片 | 主图配 FIG. 编号图注；1px 墨线描边、轻微去饱和，悬停还原 |
| 动效 | 进入时逐条错峰上浮、图片缓慢放大、标题下划线生长；`prefers-reduced-motion` 全部关闭 |
| 避免 | 紫渐变、发光霓虹、Inter/系统默认大圆角卡片、把手机页面直接放大 |

设计令牌集中在 `css/site.css` 顶部的 `:root`，页面与组件只引用令牌。

## 3. 页面清单

| 页面 | 文件 | 说明 |
|---|---|---|
| 首页 | `index.html` | 头版主稿 + 城市索引（登录后）+ 热门榜 + 最新笔记（加载更多） |
| 商户索引 | `shop-list.html` | 类别切换（登录后）、距离/人气/评分排序、商户行、分类侧栏 |
| 商户详情 | `shop-detail.html` | 图集、商户档案、经纬度、票根式代金券、秒杀状态 |
| 笔记详情 | `blog-detail.html` | 大图阅读、作者栏、关注、点赞、关联商户；当前后端要求登录 |
| 登录（验证码） | `login.html` | 左右分栏、协议勾选、发送验证码倒计时、登录后回原页 |
| 登录（密码） | `login2.html` | 同一套版式，`phone + password`（后端尚未实现密码分支） |
| 我的主页 | `info.html` | 用户档案、我的笔记、关注动态（游标分页）、退出 |
| 资料档案 | `info-edit.html` | 只读档案表（后端无更新接口） |
| 作者主页 | `other-info.html` | 作者笔记、关注/取关、共同关注（登录且非本人） |
| 写笔记 | `blog-edit.html` | 图片上传、标题/正文、商户选择弹层、发布 |

页面之外新增：

- `js/api.js`：唯一的接口访问层，一个函数对应一个既有接口。
- `js/ui.js`：工具函数、轻提示、滚动入场指令、页头/页脚/笔记卡片/星级/空状态等 Vue 组件。
- `css/site.css`：设计系统与全部页面样式。
- `imgs/icon-sprite.svg`：22 个自绘线性图标；`imgs/placeholder.svg`：图片兜底。

## 4. 接口对照（严格一一对应）

> 所有请求仍走 `/api` 前缀，`success !== true` 即 reject。
> **不再“任何 401 全局跳登录页”**：401 以 `err.auth` 交给页面处理，
> 浏览页保留可公开的数据，私有页/写操作才引导登录。
> token 仍存于 `sessionStorage.token` 并放入 `authorization` 头。

| 页面 | 接口调用（与旧版一致，按登录态条件调用） |
|---|---|
| 首页 | `GET /blog/hot?current=`；登录后才调 `GET /shop-type/list`；`PUT /blog/like/{id}`、`GET /blog/{id}` |
| 商户索引 | `GET /shop/of/type?typeId=&current=&sortBy=&x=&y=`；登录后才调 `GET /shop-type/list` |
| 商户详情 | `GET /shop/{id}`、`GET /voucher/list/{shopId}`、`POST /voucher-order/seckill/{id}` |
| 笔记详情 | `GET /blog/{id}`；登录后才调 `GET /blog/likes/{id}`、`GET /user/me`、`GET /follow/or/not/{userId}`；`PUT /follow/{userId}/{isFollow}`、`PUT /blog/like/{id}` |
| 验证码登录 | `POST /user/code?phone=`、`POST /user/login` |
| 密码登录 | `POST /user/login` |
| 我的主页 | `GET /user/me`、`GET /user/info/{id}`、`GET /blog/of/me`、`GET /blog/of/follow?offset=&lastId=`、`POST /user/logout`、`PUT /blog/like/{id}`、`GET /blog/{id}` |
| 资料档案 | `GET /user/me`、`GET /user/info/{id}`（仅在会话缓存缺失时，与原详情接口相同） |
| 作者主页 | `GET /user/{id}`、`GET /user/info/{id}`、`GET /blog/of/user?id=&current=`；登录后才调 `GET /follow/or/not/{id}`、`GET /follow/common/{id}`、`GET /user/me`；`PUT /follow/{id}/{isFollow}`、`PUT /blog/like/{id}`、`GET /blog/{id}` |
| 写笔记 | `GET /user/me`、`GET /shop/of/name?name=`、`POST /upload/blog`、`GET /upload/blog/delete?name=`、`POST /blog` |

### 与旧版行为的少量差异（均不涉及接口形态）

1. **去掉了全局 401 跳登录**。旧版 `/api` 拦截器遇到任何 401 都跳登录页，导致首页、
   商户列表这类本应可浏览的页面也被辅助请求（尤其 `/shop-type/list`）踢走。
   现在由页面判断：浏览页保留公开数据，私有页/写操作才引导登录。
2. **游客不请求已知受限的辅助接口**：首页/商户列表不请求 `/shop-type/list`；
   笔记详情不请求 `/blog/likes`、`/user/me`、关注状态；作者主页不请求关注状态与共同关注。
3. 笔记详情、作者主页：先按接口请求；若后端 401（当前拦截器如此），页面内显示
   “登录后查看”，不再整页跳转；登录后回到原页面。
4. 我的主页、资料档案、写笔记：未登录直接去登录页，带 `redirect` 参数；登录后回到原页。
5. 资料档案：会话缓存为空时用同一个 `GET /user/info/{id}` 兜底，直接打开不再空白。
6. 首页/作者页点赞前先判断登录态，避免无意义的 401。
7. 切换排序会重置分页（旧版会重复追加同一批商户）；重复 id 会被去重。
8. 退出登录无论 `POST /user/logout` 是否成功都会清理本地 token——后端该接口目前返回
   “功能未完成”，旧版会导致无法退出。
9. 移除了旧版硬编码的假评论、假口味评分；资料页改为只读档案（无写接口）。
10. 修正数据语义：`sold` 是销量（不是收藏）、`gender` 0 男 1 女、`level` 只能按 Boolean
    显示“已开通/普通用户”、不再给没有 `tb_user_info` 行的用户硬编码“杭州”。

## 5. 已知后端接口问题（前端未改接口，仅记录）

| 现象 | 原因 |
|---|---|
| 游客访问笔记详情/作者主页会落到登录提示 | `LoginInterceptor` 只放行 `/blog/hot`、`/shop/**`、`/voucher/list/**` 等；文档虽标“无需登录”，实际未放行 `/blog/{id}`、`/user/**` |
| 首页/商户列表游客看不到分类 | `/shop-type/list` 实际被登录拦截器拦截（文档标“无需登录”） |
| 关注动态提示不可用 | `BlogController` 实际映射为 `/blog/blog/of/follow`，文档与前端均为 `/blog/of/follow` |
| 共同关注提示不可用 | 后端有 `FollowServiceImpl.commonFollow`，但 `FollowController` 没有暴露路由 |
| 密码登录无法成功 | `UserServiceImpl.login` 只校验 `code`，没有读取 `password` 分支 |
| 人气/评分排序后端不生效 | `ShopController#queryShopByType` 未接收 `sortBy`；前端对**已加载数据**排序，参数仍原样透传 |
| 退出登录提示“功能未完成” | `UserController#logout` 返回 `Result.fail` |
| 详情页没有评论区 | `BlogCommentsController` 为空，评论接口未实现 |
| 资料无法保存、档案多为空 | 没有用户资料更新接口；`tb_user_info` 种子数据为空，接口返回 `data: null` |
| 粉丝/关注数不会增长 | 关注接口只写 `tb_follow` 与 Redis，没有同步 `tb_user_info.fans/followee` |
| 商户列表分页常量缺失 | `ShopServiceImpl` 引用的 `DEFAULT_BATCH_SIZE` 在源码中未定义，GEO 分页分支需后端修复 |

## 6. 桌面端适配

- 设计基准 1440 × 900，内容最大宽 1280px 居中（左右留白 80px），正文栏宽约 714px，形成“宽题窄文”的刊物节奏。
- 断点：
  - `≤1360`：栏间距收紧，笔记三栏保持；
  - `≤1120`：主栏 + 侧栏改为顺序堆叠，笔记两栏，详情/文章侧栏移到底部；
  - `≤860`：单栏、导航收紧、票据改为上下结构；仍保证无横向滚动。
- 交互按桌面习惯：固定导航、显式“继续阅读/继续翻阅”按钮（不再依赖触底滚动）、
  商户选择使用居中弹层、悬浮/聚焦状态清晰。
- 已在 1440 / 1280 / 1024 / 900 / 1920 宽度下验证 `scrollWidth == innerWidth`，无横向溢出。

## 7. 验证记录

- 使用无头 Edge（CDP）逐页截图：1440 × 1050 全套 10 页，另测 1920 / 1280 / 1024 / 900。
- 用与真实 `LoginInterceptor` 一致的本地 mock 分别验证 **游客 / 已登录** 两种状态：
  - 游客首页只请求 `/blog/hot`，分类栏显示登录入口，无全局跳转；
  - 游客商户列表只请求 `/shop/of/type`，商户正常展示；
  - 游客商户详情完全可用；
  - 游客笔记详情/作者主页各只触发一次预期 401，页面显示登录提示；
  - 游客打开我的主页/资料档案/写笔记会跳到 `/login.html?redirect=...`；
  - 登录成功后能回到 `redirect` 指定页面；
  - 已登录时笔记详情、作者主页、我的主页、写笔记正常。
- 每页检查：无 console error / warning、无横向溢出；401 仅出现在按业务应受限的页面。
- 网络请求审计：逐页记录实际 `method + path + query`，与第 4 节逐条对照；
  汇总排序、关注动态、关注状态、写笔记搜索等交互后的请求。
- 可访问性抽查：所有按钮有可读名称、表单控件有 label、无重复 id、无
  `javascript:void(0)` 假链接、装饰图片使用 `alt=""`。

> 业务与权限的完整说明见 [business-model.md](business-model.md)。
