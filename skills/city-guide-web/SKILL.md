---
name: city-guide-web
description: Design and implement the 黑马点评 city-guide web app in a Stripe-informed layout with a Vestris-informed warm editorial surface. Use when building or restyling pages, navigation, cards, forms, empty states, or design tokens for this project, or when extracting a reusable multi-page city-guide design system.
---

# City Guide Web

A design system for the 黑马点评 (Hangzhou city guide) desktop web app.  
**Structure follows Stripe. Surface follows Vestris. It is a multi-page product, never a one-page marketing clone.**

Before designing, read `references/surface-notes.md`, `references/layout-patterns.md`, `references/page-recipes.md` and `references/tokens.css`. They contain the measured reference values and the page-level mapping.

## 1. Non-negotiables

1. **Multi-page**: every route keeps its own URL and its own hero. Navigation must lead to real pages, not anchors of a single page.
2. **API parity**: never invent an endpoint or a feature. Every button must map to an existing backend action. Public/private access follows `docs/business-model.md`.
3. **Restraint with warmth**: cream, olive and ink are the dominant colors. Terracotta is an accent, not a second brand color.
4. **Readable Chinese first**: Latin display type is Instrument Serif; Chinese display falls back to Songti. Body is Manrope + system CJK sans. Never sacrifice CJK legibility for a Western font.
5. **No generic AI look**: no purple gradients, no glassmorphism everywhere, no rows of identical rounded cards without hierarchy, no one-page SaaS clone.

## 2. Direction

**Stripe layout + Vestris skin = a warm city product guide.**

- Stripe gives: left-aligned editorial hero, 12-column hairline grid, thin rules, product-UI preview, capability strip, bordered feature cards, stat band, closing CTA, big honest whitespace.
- Vestris gives: cream paper, olive primary, high-contrast serif display with italic phrase, soft organic gradient fields, 8–20px radii, quiet shadows, floating pill navigation.
- The result should feel like a well-made product site about Hangzhou life, not like a restaurant coupon page and not like a generic AI SaaS.

## 3. Tokens

Use the CSS variables in `references/tokens.css`. Key values:

```
cream #f6f3e9   paper #fbf9f3   ink #1b1c17   olive #5f6249
olive-deep #464936   muted #6d6e60   line rgba(27,28,23,.14)
sky #dfe9ee   sage #b9c4a7   sand #e7dcc6   terracotta #c46a4a
radius: sm 8 / md 12 / lg 16 / xl 24 / pill 999
shell 1240px, gutters 32–40
```

Rules:
- Background layers: cream page → paper card → olive/ink block. Use at most one dark section per page.
- Borders are 1px and low contrast; shadows are soft and large, never harsh.
- Gradients are organic and low-saturation: sage → sky → sand, or olive glow on dark. No rainbow ribbons, no neon.
- Grain: optional fixed SVG noise at 0.05–0.08 opacity. Never over text.

## 4. Typography

| Role | Font | Size / treatment |
|---|---|---|
| Hero display | Instrument Serif + Songti fallback | clamp(48px, 6vw, 86px), 400, line-height .98, letter-spacing -.02em |
| Section title | Instrument Serif / Songti | clamp(32px, 4vw, 52px), 400, line-height 1.05 |
| Card title | Manrope 600 / Songti | 18–24px, line-height 1.35 |
| Body | Manrope + PingFang/YaHei | 15.5–16.5px, line-height 1.7–1.8 |
| Eyebrow | Manrope 600 uppercase | 10.5–11.5px, letter-spacing .16em |
| Meta / numbers | Manrope tabular | 11.5–13px, letter-spacing .02em |

Composition:
- Use **one italic Latin word or phrase per hero** inside an otherwise upright line, e.g. `把城市过成 <em>a daily read</em>`. For Chinese-only headlines, keep the Songti upright.
- Mix one very large display line with small uppercase eyebrows. Avoid medium-size all-bold titles everywhere.
- Limit to 2 weights of Manrope (500/600) plus 400 body.

## 5. Navigation

The Vestris-style floating nav is the signature:

- Sticky at top with 16px top margin; inner shell max 1240, height 64–68, radius 14, background `rgba(251,249,243,.82)`, backdrop blur 18px, 1px border, soft shadow.
- Left: custom leaf/pin SVG mark + serif wordmark (Chinese, 20–22px).
- Center: 4 text links (首页 / 商户 / 热门 / 我的 / 写笔记 as needed), 14px, muted → ink on hover, no boxes.
- Right: quiet "登录" pill (cream, border) + primary "写笔记" pill (olive, cream text), 40–44px height.
- On scroll > 24px: compress to 58–60px, raise shadow, slightly more opaque.
- Mobile (<900): hide center links into a rounded sheet/menu; keep logo + primary CTA.
- Do **not** add a mega-menu or fake search. Every nav item goes to a real route/anchor.

## 6. Layout patterns (Stripe-informed)

1. **Page hero**: 12-col; 7 cols copy + 5 cols visual (or full-width copy for detail pages). Eyebrow → H1 → subcopy → 1–2 CTAs → small trust row. Bottom hairline.
2. **Capability strip**: 4 items in a row, separated by 1px rules, icon + label + one line. No fake metrics.
3. **Feature section**: left intro (eyebrow + H2 + paragraph) beside a 2-col card grid; cards contain a real UI preview or content image.
4. **Stat/gradient band**: full-bleed soft sage/sky gradient, large serif line, 3–4 real counts. Use only counts the data actually gives.
5. **Bordered panel**: pricing-like panel with rows and a right-aligned action; useful for vouchers and settings.
6. **Steps**: 01 / 02 / 03 numbered columns for publish flow or onboarding.
7. **Closing CTA**: one dark olive rounded block with a serif line and one pill CTA.

Sections breathe: 88–120px vertical padding, 1px dividers, no shadowed card walls.

## 7. Components

- **Button**: `primary` olive pill, `secondary` paper+border, `ghost` text, `danger` terracotta. Heights 40/46/52. Radius 10. Hover lifts 1px and darkens; focus ring 2px offset.
- **Card**: paper bg, 1px line, radius 16–20, soft shadow; hover translateY(-3px) + shadow grow; image top with 12px radius inner.
- **Product preview**: a rounded browser-like frame with three dots, inner screenshot-like content built from real data (images, titles, ratings). This replaces abstract illustration.
- **Shop card**: 16:10 image, name, rating, comments/sales, area/distance, price, arrow. 2 columns desktop.
- **Note card**: image 4:3, eyebrow/number, serif title, author row, like/comment. Can be grid or horizontal.
- **Voucher**: bordered panel with a perforation line, large price, discount badge, stock/time, one CTA; 2 columns.
- **Tabs / segmented control**: pill group on cream-deep with active paper pill; or underline tabs for reading pages.
- **Form**: label 11px uppercase; input 48px, 1px line, focus olive; error terracotta; form card radius 20.
- **Empty / gate**: centered paper card with a serif line, one explanation, one primary CTA, one quiet link.
- **Toast**: ink pill, cream text, radius 10, top-center or bottom-right.

## 8. Interaction & motion

- Reveal on scroll: 12px translate + opacity, 600ms, stagger 60ms; disable under `prefers-reduced-motion`.
- Hero visual: slow float (6s) and a 2–4px mouse parallax; no constant spinning.
- Nav: shrink on scroll, active link dot/underline, mobile sheet.
- Cards: image scale 1.03, lift 3px, border darkens.
- Buttons: 1px lift, 120ms; primary shadow.
- Tabs/segmented: sliding active pill or underline.
- Keep interactions predictable: no hidden primary actions, no hover-only affordances.

## 9. Page recipes

See `references/page-recipes.md` for the section-by-section mapping of all ten routes. Summary:

- Home: hero + lead preview → capability strip → hot notes grid → city index (auth-aware) → ranking → dark CTA.
- Shop list: compact hero + filters → shop card grid + sticky ranking aside → load more.
- Shop detail: gallery + purchase info hero → facts → vouchers → note.
- Blog detail: article hero → image → prose + sticky rail (author/like/shop).
- Login: split brand panel + form card; bare nav.
- Profile: dashboard hero + stats + tabs + notes/feed.
- Info-edit: settings cards + summary sidebar.
- Author: profile hero + follow + notes + common-follow.
- Write: composer card + sticky publish/shop sidebar.
- Gates: paper card with login CTA, never a full-page redirect for public browsing routes.

## 10. Accessibility & responsive

- One `h1` per page; gate states keep an sr-only h1.
- Focus-visible olive ring; all icon buttons have labels.
- Contrast: body ink on cream ≥ 7:1; muted meta ≥ 4.5:1.
- Breakpoints: 1200 / 1000 / 820 / 640. Desktop grid → stacked; nav → sheet; cards 3→2→1.
- No horizontal scroll at 1024 / 1280 / 1440 / 1920.

## 11. Avoid

- One-page long scroll as the whole product; nav anchors pretending to be routes.
- Copying Vestris's centered hero layout or its botanical photo collage.
- Stripe's purple/blue palette and gradient-ribbon hero.
- Injecting fake stats, fake comments, fake categories, or fake actions.
- Dense dashboards on reading pages, or marketing whitespace on editor pages.

## 12. Checklist

- [ ] Every nav item and CTA goes to an existing page/endpoint.
- [ ] Public pages remain browsable; private actions gate with redirect-back.
- [ ] Nav is floating, sticky, shrinks, and works on mobile.
- [ ] One dark section max; cream/olive/ink dominant.
- [ ] Display type mixes Instrument Serif + Songti without breaking CJK.
- [ ] Section rhythm is Stripe-like: left aligned, hairline separated, not card-walled.
- [ ] No invented data or API.
- [ ] Screenshots at 1440 + 1024 verified; no console errors.
