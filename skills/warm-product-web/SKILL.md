---
name: warm-product-web
description: Design warm editorial product websites that pair Stripe-informed multi-page layout with a cream/olive, serif-led surface. Use for product marketing, content platforms, dashboards, forms, navigation systems, and reusable design tokens.
---

# Warm Product Web

A portable design system for warm, editorial product sites.  
**Layout from Stripe. Surface from Vestris. Multi-page, never a one-page clone.**

## When to use

- A product, content or community site with real routes: home, list, detail, article, auth, dashboard, editor.
- You want a distinctive alternative to generic SaaS: cream paper, olive ink, large serif display, quiet motion.
- Not for dense trading/ops consoles where data density must outrank editorial rhythm.

## Non-negotiables

1. **Multi-page.** Every nav item and CTA goes to a real route. One `h1` per route.
2. **Structure before decoration.** Left-aligned grid, hairline rules, real UI previews. No ornamental blobs without content.
3. **No invented data.** Do not fake metrics, logos, testimonials, categories, prices or features.
4. **Warm restraint.** Cream/olive/ink dominate. One dark section per page max. Terracotta is an accent, not a second brand.
5. **Typography first.** Self-host fonts with an OFL-compatible license; verify the locale/CJK fallback before shipping.
6. **Responsive and accessible.** No horizontal scroll; visible focus; labelled controls; body contrast ≥ 4.5:1; honor `prefers-reduced-motion`.

## Tokens

Use [`references/tokens.css`](references/tokens.css). Core palette:

| Role | Value |
|---|---|
| Canvas / card | `#f6f3e9` / `#fbf9f3` |
| Ink / muted | `#1b1c17` / `#6d6e60` |
| Primary / deep | `#5f6249` / `#464936` |
| Accent | `#c46a4a` |
| Atmosphere | sky `#dfe9ee`, sage `#b9c4a7`, sand `#e7dcc6` |
| Line | `rgba(27,28,23,.14)` |

Radii 8 / 12 / 16 / 24 / pill. Soft shadows only. Shell 1240px, gutters 32–40.

## Typography

| Role | Rule |
|---|---|
| Display | Instrument Serif + locale serif; clamp(44px, 6vw, 88px); 400; line-height ~1; tracking −.02em |
| Section title | Same family; clamp(32px, 4vw, 54px) |
| Body | Manrope + locale sans; 15.5–17px; line-height 1.7–1.85 |
| UI / labels | Manrope 500–600; 12–14px |
| Eyebrow | Uppercase; 10–12px; tracking .16em |
| Numbers | Tabular figures; never fake precision |

Use **one italic Latin phrase per hero** inside an otherwise upright line. Do not italicize CJK display text artificially.

## Navigation

The floating pill nav is the signature:

- Sticky, 16px top offset; inner shell max 1240px; height 64–66px; radius 14px.
- `rgba(251,249,243,.82)` + `backdrop-filter: blur(18px)` + 1px border + soft shadow.
- Left: small custom mark + serif wordmark. Center: 3–5 plain links. Right: quiet login pill + olive primary pill.
- On scroll > 24px: height 56–60px, more opaque, stronger shadow.
- Below 1000px: collapse links into a rounded sheet; keep logo + primary CTA.
- No mega-menu, fake search or nav item that only scrolls to a marketing section.

## Page patterns

| Pattern | Shape |
|---|---|
| Hero | 7/5 grid: eyebrow → 2-line display → subcopy → 2 CTAs; product preview at right |
| Capability strip | 4 real capabilities separated by hairlines; icon + label + one line |
| Feature section | Left intro + 2- or 3-column cards containing real UI/content |
| Content grid | Cards or hairline lists; 3→2→1 columns |
| Detail | Media + sticky info column; facts row; bordered panel for options |
| Article | Large title, media, 680–720px prose + 320px sticky rail |
| Auth | Split: olive brand panel + paper form card; one primary action |
| Dashboard | Profile/header + stat strip + tabs + list + sidebar |
| Editor | Main composer card + sticky publish/settings sidebar |
| Empty / gate | Paper card, one explanation, one primary CTA |
| Closing CTA | One dark olive block with a serif line and one action |

Sections breathe: 72–110px vertical padding, 1px dividers. Avoid stacking five identical card walls.

## Components

- **Buttons:** primary olive, secondary paper+border, ghost text. 40/46/52px heights; radius 10; 1px hover lift.
- **Cards:** paper, 1px line, 16–20px radius; hover lift 3px; image scale 1.03.
- **Pills / tags:** 28–32px, radius 999; use for metadata, not for every action.
- **Forms:** 48–50px inputs, 1px border, olive focus ring, label above; errors in terracotta.
- **Panels:** bordered rows with a right-aligned action for pricing, settings, vouchers, facts.
- **Preview frames:** a rounded browser-like panel with dots, URL and real content; soft shadow.

## Motion

- Reveal: 12px translate + opacity, 600ms, 60ms stagger; IntersectionObserver.
- Nav shrink on scroll; card hover lift; image scale.
- One high-impact hero entrance, not scattered micro-animations.
- Disable all non-essential motion under `prefers-reduced-motion`.

## Avoid

- Purple/blue SaaS gradients, neon, glassmorphism everywhere.
- One-page marketing structure disguised as a product.
- Centered hero composition for product pages.
- Uniform rounded cards with no hierarchy; shadows on every surface.
- Inter/Roboto/Arial defaults; fake stats; invented features or endpoints.
- Copying a reference brand's assets, wordmark, copy or illustration.

## Checklist

- [ ] Every route has one h1 and a real nav destination.
- [ ] No fake data, links or features.
- [ ] Cream/olive/ink dominate; dark section ≤ 1.
- [ ] Floating nav shrinks and collapses correctly.
- [ ] Display + body fonts render in the target language.
- [ ] 1440 / 1024 / 360 verified; no overflow.
- [ ] Focus, labels, contrast and reduced motion checked.

Reference measurements: [`references/notes.md`](references/notes.md).
