# 项目设计技能

本目录存放本项目的前端设计规范，供后续迭代、重构或新增页面时复用。

| 技能 | 用途 |
|---|---|
| [`city-guide-web`](city-guide-web/SKILL.md) | 近邻生活的 Stripe 版式 + Vestris 视觉体系：令牌、导航、组件、页面配方与检查清单 |

建议使用顺序：

1. 读 `city-guide-web/SKILL.md` 的方向与硬性约束；
2. 读 `city-guide-web/references/page-recipes.md` 确认页面逐节结构；
3. 从 `city-guide-web/references/tokens.css` 取令牌；
4. 实现时复用 `frontend/html/hmdp/css/site.css` 与 `js/ui.js`，不要在页面里新造颜色、圆角或阴影。

完整重写说明见 [`docs/frontend-desktop-redesign.md`](../docs/frontend-desktop-redesign.md)，
业务与登录模型见 [`docs/business-model.md`](../docs/business-model.md)。
