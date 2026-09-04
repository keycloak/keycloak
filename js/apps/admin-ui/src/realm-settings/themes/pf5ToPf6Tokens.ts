// PF5 global token suffixes (without --pf-v5-global--) mapped to PF6 token suffixes
// (without --pf-t--global--). Used by login preview and quick-theme export.
const PF5_TO_PF6_TOKEN_MAP: Record<string, string> = {
  "FontFamily--text": "font--family--body",
  "danger-color--100": "color--status--danger--default",
  "success-color--100": "color--status--success--default",
  "primary-color--100": "color--brand--default",
  "primary-color--200": "color--brand--hover",
  "primary-color--300": "color--brand--default",
  "active-color--100": "color--brand--clicked",
  "link--Color": "text--color--link--default",
  "link--Color--hover": "text--color--link--hover",
  "BackgroundColor--light-100": "background--color--100",
  "BackgroundColor--100": "background--color--100",
  "BackgroundColor--300": "background--color--200",
  "BackgroundColor--400": "background--color--400",
  "BackgroundColor--dark-100": "background--color--highlight--default",
  "BackgroundColor--dark-300": "background--color--disabled--default",
  "palette--black-1000": "background--color--primary--default",
  "Color--100": "text--color--100",
  "Color--light-100": "text--color--inverse",
  "Color--light-200": "icon--color--200",
  "Color--dark-100": "text--color--200",
};

export function toPf6CssVar(pf5Suffix: string): string | undefined {
  const token = PF5_TO_PF6_TOKEN_MAP[pf5Suffix];
  return token ? `--pf-t--global--${token}` : undefined;
}

export function pf5VarsToPf6Css(obj?: object): string {
  return Object.entries(obj || {})
    .map(([key, value]) => {
      const cssVar = toPf6CssVar(key);
      return cssVar ? `${cssVar}: ${value};` : undefined;
    })
    .filter((line): line is string => line !== undefined)
    .join("\n");
}

export function pf5VarsToPf5Css(obj?: object): string {
  return Object.entries(obj || {})
    .map(([key, value]) => `--pf-v5-global--${key}: ${value};`)
    .join("\n");
}
