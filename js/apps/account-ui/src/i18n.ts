import { createInstance } from "i18next";
import HttpBackend from "i18next-http-backend";
import { initReactI18next } from "react-i18next";

import { environment } from "./environment";
import { joinPath } from "./utils/joinPath";

const DEFAULT_LOCALE = "en";
const DEFAULT_NAMESPACE = "translation";

// This type is aliased to any, so that we can find all the places where we use it.
// In the future all casts to this type should be removed from the code, so
// that we can have a proper type-safe translation function.
export type TFuncKey = any;

export const i18n = createInstance({
  defaultNS: DEFAULT_NAMESPACE,
  fallbackLng: DEFAULT_LOCALE,
  ns: [DEFAULT_NAMESPACE],
  interpolation: {
    escapeValue: false,
  },
  backend: {
    loadPath: joinPath(environment.resourceUrl, "locales/{{lng}}/{{ns}}.json"),
  },
});

i18n.use(HttpBackend);
i18n.use(initReactI18next);
