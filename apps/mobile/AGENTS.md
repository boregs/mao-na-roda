# Mão na Roda — Mobile UI Instructions

## Purpose

Maintain the **sage + forest** identity throughout the Android Flutter app.

The visual personality is approachable, clear, dependable, and practical. Accessibility, readable information, and comfortable interaction are central to the design.

Apply these instructions to new screens and changes to existing UI. Preserve unrelated developer work.

For every frontend task, also read `skills/frontend/SKILLS.md`.
It requires market-reference research, prioritizing Mobbin, and a written
critique of the proposed and final UI.

## Inspect and reuse before creating

Before changing UI, inspect:

- `lib/app_theme.dart`
- `lib/design_system/`
- The relevant feature under `lib/features/`
- Existing shared widgets and dependencies
- `../../docs/02-regras-de-negocio.md`
- `../../docs/03-requisitos.md`

Use `Theme.of(context).colorScheme` and `Theme.of(context).textTheme`.

Define reusable styling centrally. Avoid screen-specific copies of colors, typography, button styles, and spacing rules. If a semantic token is missing, add it to the shared design system instead of inventing a local value.

## Color identity

| Role | Light | Dark |
|---|---|---|
| Primary action / selection | `#375534` | `#AEC3B0` |
| Content on primary | `#E3EED4` | `#0F2A1D` |
| Main background | `#F7FBF4` | `#0F2A1D` |
| Cards and raised surfaces | `#D8E5CD` | `#193523` |
| Main text | `#0F2A1D` | `#E3EED4` |
| Supporting text | `#375534` | `#AEC3B0` |
| Selected-container tint | `#AEC3B0` | `#375534` |
| Decorative accent | `#6B9071` | `#6B9071` |

Rules:

- Use tinted surfaces instead of absolute white backgrounds.
- Use forest green to establish hierarchy and sage for selected containers.
- Brand green means action or selection, not verified accessibility.
- Keep attention, error, and verification states semantically separate.
- Communicate status with labels and icons as well as color.
- Define foreground/background pairs together.
- Support system light and dark modes for every screen.
- Avoid large saturated backgrounds, decorative gradients, heavy shadows, and translucent reading surfaces unless explicitly requested.

`ColorScheme.fromSeed` does not guarantee that `primary` equals its seed. Where exact brand roles are needed, set them centrally and explicitly. Do not compensate with hardcoded colors inside individual screens.

## Typography

Use the locally bundled **Source Serif 4** family through the app theme for all titles, body text, controls, and supporting information. Use its heavier weights for hierarchy and its text-oriented optical sizing for readable body copy.

| Role | Size | Weight |
|---|---:|---:|
| Welcome/display heading | 40sp | Semibold |
| Main heading | 32sp | Semibold |
| Section heading | 26–28sp | Semibold |
| Card/app-bar heading | 20–24sp | Semibold |
| Body | 18–20sp | Regular |
| Control label | 18sp | ExtraBold |
| Supporting metadata | 16sp | Regular |

- Map these roles centrally to `TextTheme`.
- Use sentence case and comfortable line spacing.
- Respect Android text scaling.
- Let text wrap and containers grow.
- Never fit text by shrinking it or disabling scaling.

## Spacing and components

Use the spacing scale **4, 8, 12, 16, 24, 32dp**.

- Screen horizontal padding: 24dp, adapting when needed.
- Input and button radius: 12dp.
- Card radius: 16dp.
- Bottom-sheet top corners: 24dp.
- Primary button minimum height: 56dp.
- Interactive touch targets: at least 48 × 48dp.

Use Material components with centralized themes. Every interactive component must handle its relevant enabled, disabled, pressed, focused, selected, loading, and error states.

Give each screen or task one visually dominant primary action.

## Icons and branding

- Use a consistent rounded Material icon treatment.
- Prefer outlined ordinary controls and filled selected states.
- Typical icon size is 24dp inside a larger touch target.
- Label unfamiliar actions and accessibility categories.
- Keep decorative icons out of redundant screen-reader announcements.

Reuse approved image assets from `assets/images/` and `BrandMark`. The continuous-path visual motif represents movement and connection, but do not create placeholder hero artwork. Keep decorative paths outside operational map content.

Asset rules:

- Branding images belong in `assets/images/branding/`.
- Onboarding images belong in `assets/images/onboarding/`.
- Reusable illustrations belong in `assets/images/illustrations/`.
- Keep explanatory text out of images.
- Use `_light` and `_dark` suffixes only for genuinely theme-specific artwork.
- Register populated directories in `pubspec.yaml` and preserve licenses for bundled assets.

The app currently uses Flutter’s built-in Material icons. A migration to Material Symbols Rounded must be coordinated through shared icon definitions; do not introduce competing icon packages per feature.

## Accessibility and layout

Meet the project’s accessibility requirements:

- Normal text contrast: at least 4.5:1.
- Large text and essential graphical controls: at least 3:1.
- Meaningful TalkBack labels, heading semantics, and reading order.
- Usable layouts at 200% text scaling.
- Reachable primary actions and explicit alternatives to gesture-only controls.
- Android back behavior and safe-area handling.
- Reduced-motion support whenever animation is added.

For vertically flexible screens, use a layout that fills available space and scrolls when content grows.

**Never put `Spacer` or `Expanded` in a vertically unbounded `Column`.** A `ConstrainedBox` with only `minHeight` does not solve this. Use a bounded layout or the existing `SliverFillRemaining(hasScrollBody: false)` pattern.

## Product language and information hierarchy

All user-facing copy is **Brazilian Portuguese**. Prefer direct language such as:

- “Continuar”
- “Consultar rota”
- “Ver condições do caminho”
- “Relatar barreira”
- “Como funciona”

Represent actual feature availability accurately.

For routes and barriers, distinguish:

1. The reported or recorded condition.
2. Its effect on the selected mobility profile.
3. Its source and verification date.
4. Whether a user report is validated.
5. Whether information is outdated.

Severity and verification are independent. A serious reported obstacle can still be unvalidated. Styling changes must not silently change product behavior or business rules.

## Onboarding conventions

Maintain a short first-run experience:

- One welcome screen.
- One primary “Continuar sem conta” action.
- One outlined “Criar conta” action.
- One text “Já tenho conta · Entrar” action.
- Optional “Como funciona” explanation.
- A locally stored completion flag.
- Recoverable startup when preferences are unavailable.
- An accessible way to revisit explanatory content.

Onboarding completion is independent of permission consent. Request location when the user chooses a location-dependent action.

## Verification before completion

For UI changes:

1. Run formatting and `flutter analyze`.
2. Run relevant existing tests.
3. Add regression coverage for layout, navigation, or state bugs.
4. Review affected screens in light and dark modes.
5. Check small-screen layout, enlarged text, contrast, and reachable controls.
6. Verify Android behavior on a device or emulator when available.

Do not claim visual or runtime verification based only on source inspection or `git diff --check`. If tooling is unavailable, report exactly which checks could not run.

## Managing design changes

- Extend the established system rather than creating feature-specific themes.
- Treat changes to brand colors, font families, icon families, or core geometry as app-wide design decisions.
- Apply approved changes through shared tokens and components.
- Update this file when an approved design decision changes.
- Flag conflicts between this specification and the implementation; avoid silently reproducing known inconsistencies.

**Success criterion:** every screen should feel like part of the same app, regardless of which developer or agent created it.
