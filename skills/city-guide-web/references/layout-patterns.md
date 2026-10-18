# Stripe layout patterns (measured & translated)

Source: https://stripe.com, captured at 1440×900 on 2026-10-18.

## Measured basics

| Item | Stripe | Translation for this app |
|---|---|---|
| Header height | 76px, white, thin bottom rule | floating nav 64px on cream, no full white bar |
| Content width | ~1232px, 12-col grid | 1240px shell |
| Hero headline | 48px / 55px, weight 300, left aligned, gradient text | 48–86px Instrument Serif/Songti, 400, left aligned, olive/ink, italic accent |
| Hero CTAs | primary purple pill 8px radius + ghost with border | primary olive pill + secondary cream pill |
| Trust row | logo grid inside hairline borders | capability strip: 4 real features, hairline separated |
| Section intro | left-aligned 24–28px sentence over cards | left intro (eyebrow + H2 + paragraph) beside card grid |
| Cards | white, 1px border, 8px radius, product UI visual | paper, 1px border, 16–20px radius, real content preview |
| Feature split | 2/3 + 1/3, 2-col and 3-col grids | 7/5 hero, 2-col cards, 3-col notes |
| Pricing | bordered panel with rows and right-aligned CTA | vouchers/settings panel |
| Steps | 01/02/03 columns with colored numerals | publish steps / onboarding |
| Footer | dark, multi-column, dense links | dark olive, 4 columns, serif wordmark |
| Motion | subtle reveal, hover, accordions | reveal + lift + nav shrink; no heavy parallax |

## Structural rules

1. **Left aligned, not centered.** Headlines, intros and section copy start at the left grid edge. Centering is reserved for auth gates and small empty states.
2. **Rules before shadows.** Sections are separated by 1px hairlines and generous padding. Cards only get a soft shadow when they are interactive.
3. **Show the product.** Every important section contains a real interface/content preview (note image, shop card, voucher panel), not abstract decoration.
4. **One idea per section.** Intro sentence → supporting cards → CTA. Don't stack three headings.
5. **Density alternates.** Big display + airy hero, then a compact capability strip, then a card grid, then a dark CTA. Avoid uniform card rhythm for 5 screens.
6. **Numbers only when true.** If the API does not provide a total, do not print one. Use loaded counts with explicit labels ("已加载 5 家").
7. **CTA hierarchy.** One primary action per viewport; secondary actions are text/outline. The primary CTA is never duplicated three times in one section.
8. **Hairline grid.** Use a max-width shell, visible left/right rules on feature sections where useful, and 1px internal dividers.

## Translated section vocabulary

- `page-hero` — eyebrow, display, sub, CTA, visual.
- `capability-strip` — four real capabilities.
- `split-intro` — left intro + right cards.
- `content-grid` — notes or shops.
- `panel` — vouchers, settings, facts.
- `ranking` — numbered rows with meta.
- `dark-cta` — closing action block.
- `auth-gate` — centered paper card.

## Not to copy

- Purple/blue palette, gradient ribbon artwork, logo wall of fake customers.
- The centered one-page marketing narrative.
- 76px white nav bar; ours is a floating cream pill.
- Dense tiny type; our Chinese body needs 15.5–16.5px.
