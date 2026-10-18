# Page recipes

Every page keeps its existing URL, API calls and access rules. These are content-order recipes, not new features.

## Home / `index.html`

1. Floating nav.
2. Hero (7/5): eyebrow “杭州 · 城市生活向导”; display “把城市生活，读成 <em>一份指南</em>”; subcopy; primary “探索美食商户” → `/shop-list.html?type=1&name=美食`; secondary “读热门笔记” → `#hot`; trust line.
   Right: product preview card — lead note image, author, likes, floating chip. Built from `GET /blog/hot`.
3. Capability strip: 真实探店笔记 / 按类别找店 / 限时优惠券 / 关注作者（no fake numbers）.
4. Hot notes section (`#hot`): left intro + 3-col note grid; load more via `GET /blog/hot?current=`.
5. City index section: logged → category cards from `GET /shop-type/list`; guest → login gate panel (backend blocks this endpoint).
6. Ranking section: top 5 by likes from loaded hot notes; two-column intro + rows.
7. Steps band: 01 read / 02 find a shop / 03 write a note (real routes only).
8. Dark CTA: “把你的城市发现写下来” → `/blog-edit.html` (auth-gated).
9. Footer.

## Shop list / `shop-list.html`

1. Nav → breadcrumb.
2. Compact hero: eyebrow “MERCHANTS / 分类索引”, H1 type name, meta “已加载 n 家 · 第 p 页”, type picker + sort segmented + coordinates/hint.
3. Shop card grid (2 cols): image, name, rating, comments, sales, area, distance, price, arrow. Uses `GET /shop/of/type`.
4. Sticky aside: “本页人气” ranking + “换一类” (logged) / login prompt (guest).
5. Load more. Footer.

## Shop detail / `shop-detail.html`

1. Breadcrumb + product hero: left gallery (main image + thumbs, rounded frame); right info (eyebrow, H1, rating, comments/sales, price/open hours, address, coordinates, CTAs).
2. Facts panel (3 rows/2 cols): 人均 / 区域 / 营业时间 / 已售.
3. Vouchers: intro + 2-col voucher cards. Price, discount, rules, stock/time, one CTA; login gate only on seckill.
4. Note: data provenance; footer.

## Blog detail / `blog-detail.html`

1. Breadcrumb + article hero: eyebrow, display title, author row + follow, date.
2. Large image with caption (gallery thumbs), rounded 24px frame.
3. Prose (max 720) + sticky rail: author card, like card, shop card, info.
4. End action bar.
5. If backend 401 → auth gate card inside the page.

## Login / `login.html`, `login2.html`

1. Bare nav (logo + back).
2. Split: left olive/ink brand panel with serif quote and soft gradient; right centered form card (paper, 24px radius, soft shadow).
3. Mode switch pills, labeled fields, primary button, legal copy.
4. Redirect back to `?redirect=` after login.

## Profile / `info.html`

1. Nav.
2. Dashboard hero: avatar, name, UID/city, stat cards (近期笔记 / 粉丝 / 关注), actions (编辑 / 退出).
3. Tabs: 我的笔记 / 关注动态.
4. Notes grid or horizontal rows; feed cursor list. Empty/error states.
5. Sidebar profile card / writing CTA.
6. Footer.

## Info edit / `info-edit.html`

1. Nav + compact hero “资料档案”.
2. Settings panel: labeled read-only rows in a paper card; summary sidebar with avatar, UID, note about read-only.
3. Footer.

## Author / `other-info.html`

1. Nav + profile hero: avatar, name, city, stats, follow button (hidden for self).
2. Tabs: notes / common follow (logged, non-self).
3. Notes grid/list; common-follow cards; gates when protected.
4. Footer.

## Write / `blog-edit.html`

1. Nav + compact hero.
2. Two columns: main composer card (uploader, title, content, counters); sticky sidebar (shop picker, publish, tips).
3. Shop picker as modal.
4. Footer.

## Auth gates

Use one consistent `.gate-card` on paper:
- Kicker “MEMBERS ONLY” or “SIGN IN”.
- Serif line: what is protected.
- One sentence explaining redirect-back.
- Primary “登录后继续” + quiet “回到首页”.
- Never use a full-page redirect for a browsing route.
