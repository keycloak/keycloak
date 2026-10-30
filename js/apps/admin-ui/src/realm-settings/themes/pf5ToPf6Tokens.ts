/**
 * Maps Quick Theme PF5 global variable suffixes (without `--pf-v5-global--`) to
 * PatternFly 6 semantic token suffixes (without `--pf-t--global--`).
 *
 * PF6 has no 1:1 replacement for PF5 globals — choose the semantic token that
 * matches the Quick Theme control's purpose (see PatternflyVars.ts). Prefer
 * semantic tokens (no trailing number) over palette/base tokens.
 *
 * Light vs dark is not encoded in these names. `BackgroundColor--dark-*` is a
 * PF5 palette shade used in the light theme (e.g. nav/header). Dark mode is
 * applied separately by writing `styles.dark` under `.pf-v6-theme-dark`.
 *
 * The Admin UI login preview still renders PF5 React components, so callers also
 * emit the original PF5 variables via `pf5VarsToPf5Css` until that preview moves
 * to PF6.
 *
 * @see https://www.patternfly.org/foundations-and-styles/design-tokens/develop/
 * @see https://www.patternfly.org/foundations-and-styles/colors/
 */
const PF5_TO_PF6_TOKEN_MAP: Record<string, string> = {
  // Typography
  "FontFamily--text": "font--family--body",

  // Status
  "danger-color--100": "color--status--danger--default",
  "success-color--100": "color--status--success--default",

  // Brand / actions (primaryColor, hover, active)
  "primary-color--100": "color--brand--default",
  "primary-color--200": "color--brand--hover",
  // Dark-theme primary uses PF5's 300 shade; same brand role in PF6
  "primary-color--300": "color--brand--default",
  "active-color--100": "color--brand--clicked",

  // Links
  "link--Color": "text--color--link--default",
  "link--Color--hover": "text--color--link--hover",

  // Page / shell backgrounds (from backgroundColor + dependencies)
  // Main content surface
  "BackgroundColor--light-100": "background--color--primary--default",
  // Dark-theme accent / elevated surface (keep distinct from nav)
  "BackgroundColor--300": "background--color--floating--default",
  // Form control surface (input background in dark theme)
  "BackgroundColor--400": "background--color--control--default",
  // Nav / shell (distinct from header so both controls survive export)
  "BackgroundColor--dark-300": "background--color--secondary--default",
  "BackgroundColor--100": "background--color--secondary--default",
  // Header / masthead (distinct from nav)
  "BackgroundColor--dark-100": "background--color--tertiary--default",
  "palette--black-1000": "background--color--tertiary--default",

  // Text / icons
  "Color--100": "text--color--regular",
  "Color--light-100": "text--color--inverse",
  "Color--light-200": "icon--color--inverse",
};

/** PF5 suffixes that must not share a global PF6 text token with Color--100. */
const PF5_TO_PF6_COMPONENT_OVERRIDES: Record<string, string[]> = {
  // inputTextColor: keep distinct from text--color--regular
  "Color--dark-100": ["--pf-v6-c-form-control--Color"],
};

export function toPf6CssVar(pf5Suffix: string): string | undefined {
  const token = PF5_TO_PF6_TOKEN_MAP[pf5Suffix];
  return token ? `--pf-t--global--${token}` : undefined;
}

export function pf5VarsToPf6Css(obj?: object): string {
  return Object.entries(obj || {})
    .flatMap(([key, value]) => {
      if (Object.hasOwn(PF5_TO_PF6_COMPONENT_OVERRIDES, key)) {
        return PF5_TO_PF6_COMPONENT_OVERRIDES[key].map(
          (cssVar) => `${cssVar}: ${value};`,
        );
      }
      const cssVar = toPf6CssVar(key);
      return cssVar ? [`${cssVar}: ${value};`] : [];
    })
    .join("\n");
}

/** Keep emitting PF5 variables while the Admin UI login preview still uses PF5 components. */
export function pf5VarsToPf5Css(obj?: object): string {
  return Object.entries(obj || {})
    .map(([key, value]) => `--pf-v5-global--${key}: ${value};`)
    .join("\n");
}
