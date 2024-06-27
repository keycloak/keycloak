import { LanguageDetectorModule, createInstance } from "i18next";
import HttpBackend from "i18next-http-backend";
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
  interpolation: {
    escapeValue: false,
  },
  backend: {
    loadPath: joinPath(
      environment.serverBaseUrl,
      `resources/${environment.realm}/account/{{lng}}`,
    ),
    parse: (data: string) => {
      const messages = JSON.parse(data);

      const result: Record<string, string> = {};
      messages.forEach((v: KeyValue) => (result[v.key] = v.value));
      return result;
    },
  },
});

i18n.use(HttpBackend);
i18n.use(keycloakLanguageDetector);
i18n.use(initReactI18next);
