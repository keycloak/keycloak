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
  "link--Color": "color--link--default",
  "link--Color--hover": "color--link--hover",
  "BackgroundColor--light-100": "color--background--100",
  "BackgroundColor--100": "color--background--100",
  "BackgroundColor--300": "color--background--200",
  "BackgroundColor--400": "color--background--200",
  "BackgroundColor--dark-100": "color--background--highlight--default",
  "BackgroundColor--dark-300": "color--background--disabled--default",
  "palette--black-1000": "color--background--primary--default",
  "Color--100": "color--text--regular",
  "Color--light-100": "color--text--inverse",
  "Color--light-200": "color--icon--default",
  "Color--dark-100": "color--text--subtle",
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
