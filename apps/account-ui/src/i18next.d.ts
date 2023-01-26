// https://www.i18next.com/overview/typescript
import "i18next";

import translation from "../public/locales/en/translation.json";

export type TranslationKeys = keyof translation;

declare module "i18next" {
  interface CustomTypeOptions {
    defaultNS: "translation";
    resources: {
      translation: typeof translation;
    };
  }
}
