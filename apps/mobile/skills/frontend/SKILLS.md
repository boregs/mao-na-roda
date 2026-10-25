# Frontend skills

Use these rules for every frontend screen, flow, component, or visual change.

## 1. Research market references first

Before proposing a screen, look for comparable products already on the market.
Prioritize Mobbin, especially Android references when building the Android
Flutter app. Use at least two relevant references when available.

For each reference, record:

- product and platform;
- URL;
- relevant pattern or flow;
- what will be adapted and why;
- what will deliberately not be copied.

Treat references as inspiration, not as a replacement for project requirements.
Only claim visual inspection when the screen was actually accessible. Otherwise
separate the source description from visual conclusions.

## 2. Read the project context

Before implementation, inspect the relevant feature, shared design system,
theme, existing tests, and applicable files under `docs/`. Preserve the Mão na
Roda Ocean + sunlight identity and use Brazilian Portuguese for user-facing
copy.

Use the shared theme and components. Do not introduce screen-local colors,
typography, spacing, or competing icon packages. Follow the accessibility and
layout requirements in `apps/mobile/AGENTS.md`, including 48dp touch targets,
contrast, TalkBack semantics, text scaling, keyboard-safe layouts, and light and
dark modes.

## 3. Always critique the result

Critique every proposed design and implementation before considering it done.
Review at least:

- information hierarchy and whether there is one dominant action;
- unnecessary friction and cognitive load;
- form validation, loading, empty, error, disabled, and success states;
- keyboard, back navigation, small-screen, and large-text behavior;
- contrast, focus order, labels, and screen-reader meaning;
- consistency with the existing app and the product rules;
- whether the visual treatment is an unjustified copy of a reference.

Write down concrete weaknesses, their user impact, and the correction. After
making corrections, perform a second critique rather than assuming the first
version is sufficient.

## 4. Verification

Run formatting, `flutter analyze`, and the relevant tests. Add regression tests
for changed interaction or navigation behavior. Review light and dark themes,
small screens, enlarged text, and reachable controls when tooling permits.

Do not claim a visual or runtime check that was not performed. Report blocked
checks explicitly.

## Delivery summary

For frontend work, summarize: references, design decisions, critique,
improvements, and verification results.
