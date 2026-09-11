const PREVIEW_VAR_MAP: Record<string, string> = {
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
  "BackgroundColor--300": "background--color--300",
  "BackgroundColor--dark-300": "background--color--secondary--default",
  "BackgroundColor--100": "background--color--200",
  "BackgroundColor--dark-100": "background--color--300",
  "palette--black-1000": "background--color--100",
  "BackgroundColor--400": "background--color--control--default",
  "Color--light-200": "icon--color--regular",
  "Color--100": "text--color--regular",
  "Color--light-100": "text--color--on-brand--default",
  "Color--dark-100": "text--color--regular",
};

export function toAdminPreviewCssVars(cssVars: Record<string, string>): string {
  return Object.entries(cssVars)
    .map(([key, value]) => {
      const pf6Key = PREVIEW_VAR_MAP[key];
      if (!pf6Key) {
        return null;
      }
      return `--pf-t--global--${pf6Key}: ${value};`;
    })
    .filter((line): line is string => line !== null)
    .join("\n");
}

export function getAdminPreviewBackgroundColor(
  cssVars: Record<string, string>,
): string | undefined {
  return cssVars["BackgroundColor--light-100"];
}
