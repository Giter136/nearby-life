# 前端桌面端重写说明 · Stripe × Vestris

> 分支：`feat/desktop-redesign`<br>范围：`frontend/html/hmdp/` 全部页面与静态资源（nginx 配置与部署路径不变）<br>设计规范：[`skills/city-guide-web/`](../skills/city-guide-web/SKILL.md)

---

## 1. 目标与约束

- 把原先按手机屏幕设计的 10 个静态页面，重写为**桌面端优先**的城市生活点评界面。
- **接口严格一一对应**：不新增、不改名、不合并任何后端接口；页面里每个请求路径、方法、
  查询参数都保持原有调用（仅按登录态决定“是否发起”，细节见第 5 节）。
- **浏览优先**：业务上应公开的浏览页（首页、商户列表、商户详情）游客可看；只有
  我的主页、资料档案、写笔记等私有页面以及点赞/关注/抢券/发布等写操作才要求登录。
  当前后端拦截器的真实放行范围与文档不一致，前端按“页面内提示 + 登录后回原页”处理，
  详见 [business-model.md](business-model.md)。
- 保持原有页面地址与跳转关系（`/index.html`、`/shop-list.html?type=&name=` …），
  nginx 的 `root html/hmdp` 与 `/api` 反向代理无需修改。
- 技术栈仍为静态 HTML + Vue 2 + axios，**移除 Element UI**，改为自建轻量组件与设计系统。
- **不是 one-page**：每个路由保留自己的页面、标题、主视觉与内容层级；导航指向真实页面。

## 2. 参考与转译

| 参考 | 借什么 | 不借什么 |
|---|---|---|
| [stripe.com](https://stripe.com) | 左侧对齐的大标题、12 栏细线网格、产品预览、能力条、卡片网格、编号步骤、深色收尾 CTA、滚动揭示与悬停微交互 | 紫色/蓝色渐变、渐变丝带插画、SaaS 单页叙事、白色 76px 导航 |
| [vestris.ai](https://vestris.ai) | 奶油底、橄榄绿、Instrument Serif 斜体标题、悬浮半透明胶囊导航、8–24px 圆角、柔和阴影、产品截图式视觉 | 居中的营销 hero 构图、植物照片拼贴、one-page 结构、原品牌字标 |

参考的实测颜色、导航尺寸、字体与页面配方记录在
`skills/city-guide-web/references/`：

- `surface-notes.md`：Vestris 的表面与导航测量
- `layout-patterns.md`：Stripe 的版式模式与转译规则
- `page-recipes.md`：10 个页面逐节的内容顺序
- `tokens.css`：设计令牌与自托管字体声明

## 3. 视觉方向

**「一本温暖的城市产品指南」**：用 Stripe 的产品叙事结构，承载 Vestris 的奶油、橄榄与衬线气质。

| 维度 | 选择 |
|---|---|
| 用户与任务 | 20–35 岁城市用户；逛笔记、按类别找店、抢券、写笔记、管理主页 |
| 目标感受 | 可信、松弛、有编辑感；像一份被认真设计过的城市产品，而不是优惠券站或通用 SaaS |
| 核心隐喻 | 城市索引 + 产品预览 + 纸面章节 |
| 版式 | 1240px 内容宽；页面 hero 7/5 分栏；细发丝线分隔；左对齐章节；深色收尾 |
| 色彩 | 奶油 `#f6f3e9`、纸白 `#fbf9f3`、墨 `#1b1c17`、橄榄 `#5f6249`、深橄榄 `#464936`、陶土 `#c46a4a`、雾蓝 `#dfe9ee`、鼠尾草 `#b9c4a7`、沙 `#e7dcc6` |
| 字体 | 展示：Instrument Serif（自托管，拉丁）+ Songti SC / SimSun（中文）；界面：Manrope（自托管）+ PingFang / 雅黑；数字等宽对齐 |
| 质感 | 奶油纸面、低对比 1px 线、软阴影、径向有机渐变、5% 颗粒；圆角 8–28px |
| 图片 | 使用真实笔记/商户图片；圆角 24px 画框 + 产品预览框；FIG. 编号图注 |
| 动效 | 导航滚动收缩、12px 入场、卡片抬升 3–4px、图片 1.03 倍缓慢放大、按钮 1px 抬升；`prefers-reduced-motion` 全关 |
| 避免 | 紫渐变、玻璃态堆叠、满屏同构圆角卡片、One-page、假数据、假功能 |

设计令牌集中在 `css/site.css` 顶部的 `:root`；字体文件在 `fonts/`（含 OFL 许可证）。

## 4. 页面清单与版式

| 页面 | 文件 | 版式 |
|---|---|---|
| 首页 | `index.html` | Stripe 式 hero（左文案 + 右真实笔记预览）→ 能力条 → 热门笔记三栏 → 城市索引（登录后可读）→ 点赞榜 → 三步导览 → 深色 CTA |
| 商户索引 | `shop-list.html` | 左侧对齐页头 + 类别/排序控件 → 2 栏商户卡片 + 粘性侧栏（本页人气/换一类）→ 继续翻阅 |
| 商户详情 | `shop-detail.html` | 左图集 + 右商户档案 → 四格事实面板 → 2 栏票根式代金券 → 数据说明 |
| 笔记详情 | `blog-detail.html` | 大标题阅读页 → 主图/缩略图 → 窄正文 + 粘性作者/点赞/商户栏 |
| 登录（验证码） | `login.html` | 悬浮导航 + 左右分栏：深橄榄品牌面板 + 纸面表单卡 |
| 登录（密码） | `login2.html` | 同上，`phone + password`（后端尚未实现密码分支） |
| 我的主页 | `info.html` | 档案 hero + 三格统计 → 胶囊 Tabs（我的笔记/关注动态）→ 两栏笔记 + 档案侧栏 |
| 资料档案 | `info-edit.html` | 设置页：只读信息卡 + 城市档案卡 + 侧栏摘要 |
| 作者主页 | `other-info.html` | 作者 hero + 统计 → 作者笔记/共同关注 → 档案侧栏；本人显示管理入口 |
| 写笔记 | `blog-edit.html` | 编辑器主卡（照片/标题/正文）+ 粘性侧栏（商户/发布/提示）+ 商户弹层 |

共享组件在 `js/ui.js`：悬浮导航、页脚、笔记卡片、星级、空状态、加载、轻提示、
滚动入场指令。图标在 `imgs/icon-sprite.svg`，新增叶形标志、罗盘、菜单等。

## 5. 接口对照（严格一一对应）

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
11. 新增 `hmdp:session-cleared` 事件：token 失效清理本地会话时，悬浮导航立即切回游客态，
    不会继续显示“个人主页”。

## 6. 交互清单

- **悬浮导航**：滚动 > 24px 收缩到 56px、阴影加深；移动端 < 1000px 收进汉堡菜单；
  当前页有 4px 橄榄圆点；登录后 CTA 从“登录”切换为“个人主页”。
- **滚动入场**：`v-reveal` + IntersectionObserver，卡片按列错峰 60ms；尊重减少动态偏好。
- **卡片悬停**：笔记/商户/类型卡抬升 3–4px，图片 1.03 倍放大，边框加深。
- **排序**：距离/人气/评分胶囊分段控件；参数仍发送 `sortBy`，后端不生效时只对已加载数据排序，
  并在控件右侧明确提示。
- **商户选择**：居中弹层、搜索、Esc 关闭、遮罩点击关闭、打开时锁定页面滚动。
- **阅读页**：图集缩略图切换、粘性作者/点赞栏、点赞与关注登录态预判。
- **登录页**：验证码 60 秒倒计时、协议勾选、登录后 `redirect` 回原页；密码/验证码 Tabs。
- **我的主页**：胶囊 Tabs；关注动态失败（404）时显示“暂不可用”而不是空白。
- **作者主页**：本人不显示关注入口；共同关注仅登录且非本人可见。

## 7. 已知后端接口问题（前端未改接口，仅记录）

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
| 商户列表请求失败（200 空 body） | `ShopController#queryShopByType` 调用了 `IShopService.queryShopByName(Integer,Integer,Double,Double)`，该实现直接 `return null`；应改为 `queryShopByType(...)`。请求其实已到后端，只是 controller 没进 service，所以没有 SQL 日志 |
| 商户列表按距离为空 | 需要 Redis 中存在 `shop:geo:{typeId}`；可运行 `HmDianPingApplicationTests#test3` 导入 |

## 8. 桌面端适配

- 设计基准 1440 × 900；内容最大宽 1240px（左右留白 40px + 居中），正文栏约 720px。
- 断点：
  - `≤1200`：栏距收紧，类型卡 5→4 列；
  - `≤1000`：导航链接收进菜单，hero 单栏，商户卡单栏，文章/设置/编辑器侧栏移到底部；
  - `≤820`：单栏、导航更紧凑、类型卡 2 列、票根上下结构、表单全宽；
  - 已在 1440 / 1280 / 1024 / 900 下验证 `scrollWidth == innerWidth`，无横向溢出。
- 交互按桌面习惯：粘性导航、显式“继续阅读/继续翻阅”按钮（不依赖触底加载）、
  商户选择居中弹层、键盘 Esc 关闭、清晰的 hover/focus 状态。

## 9. 验证记录

- 使用无头 Edge（CDP）逐页截图：1440 × 900 全套页面，另测 1024 / 900；截图在
  `.preview-shots/v2/`（已 gitignore）。
- 用与真实 `LoginInterceptor` 一致的本地 mock 分别验证 **游客 / 已登录 / token 过期**：
  - 游客首页只请求 `/blog/hot`；分类区显示登录提示；
  - 游客商户列表只请求 `/shop/of/type`；**mock 下商户正常展示**。真实后端该路由当前返回空 body（见第 7 节 controller 调错方法），修复前页面会提示“请求失败”；
  - 游客商户详情完全可用；
  - 游客笔记详情/作者主页各只触发一次预期 401，页面内显示登录提示；
  - 游客打开我的主页/资料档案/写笔记会跳到 `/login.html?redirect=...`；
  - 登录成功后回到 `redirect` 指定页面；
  - 已登录时首页分类、商户列表分类、笔记详情、作者主页、我的主页、写笔记正常；
  - token 过期时浏览页保留内容并切回游客导航，私有页回登录。
- 交互验证：关注动态 404 提示、按人气排序（请求带 `sortBy=comments` 并按已加载数据排序）、
  移动端汉堡菜单、登录回跳、导航滚动收缩。
- 每页检查：无 console error / warning、无横向溢出、无重复 id、按钮均有可读名称、
  表单控件有 label、装饰图片 `alt=""`；401 仅出现在按业务应受限的页面。
- 网络请求审计：逐页记录实际 `method + path + query`，与第 5 节逐条对照。

## 10. 设计规范沉淀

`skills/city-guide-web/` 是本轮设计提炼出的可复用技能文件夹：

```
skills/city-guide-web/
├─ SKILL.md                    方向、令牌、导航、组件、页面配方、检查清单
└─ references/
   ├─ tokens.css               颜色/字体/圆角/阴影/断点变量
   ├─ surface-notes.md         Vestris 的实测表面与导航记录
   ├─ layout-patterns.md       Stripe 版式模式与转译规则
   └─ page-recipes.md          10 个页面的逐节配方
```

后续新增页面时，先读 `SKILL.md` 与 `page-recipes.md`，复用 `site.css` 的令牌和
`ui.js` 的组件；不要往页面里硬编码颜色、圆角或阴影。

> 业务与权限的完整说明见 [business-model.md](business-model.md)。
