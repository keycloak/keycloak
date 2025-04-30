import { LanguageDetectorModule, createInstance } from "i18next";
import FetchBackend from "i18next-fetch-backend";
import { initReactI18next } from "react-i18next";

import { environment } from "./environment";
import { joinPath } from "./utils/joinPath";

const DEFAULT_LOCALE = "en";

type KeyValue = { key: string; value: string };

// This type is aliased to any, so that we can find all the places where we use it.
// In the future all casts to this type should be removed from the code, so
// that we can have a proper type-safe translation function.
export type TFuncKey = any;

export const keycloakLanguageDetector: LanguageDetectorModule = {
  type: "languageDetector",

  detect() {
    return environment.locale;
  },
};

export const i18n = createInstance({
  fallbackLng: DEFAULT_LOCALE,
  nsSeparator: false,
  interpolation: {
    escapeValue: false,
  },
  backend: {
    loadPath: joinPath(
      environment.serverBaseUrl,
      `resources/${environment.realm}/account/{{lng}}`,
    ),
    parse(data: string) {
      const messages: KeyValue[] = JSON.parse(data);

      return Object.fromEntries(messages.map(({ key, value }) => [key, value]));
    },
  },
});

i18n.use(FetchBackend);
i18n.use(keycloakLanguageDetector);
i18n.use(initReactI18next);
