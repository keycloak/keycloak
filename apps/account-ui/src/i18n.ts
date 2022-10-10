import { createInstance } from "i18next";
import HttpBackend from "i18next-http-backend";
import { initReactI18next } from "react-i18next";

import { environment } from "./environment";

const DEFAULT_LOCALE = "en";
const DEFAULT_NAMESPACE = "translation";

export const i18n = createInstance({
  defaultNS: DEFAULT_NAMESPACE,
  fallbackLng: DEFAULT_LOCALE,
  ns: [DEFAULT_NAMESPACE],
  interpolation: {
    escapeValue: false,
  },
  backend: {
    loadPath: environment.resourceUrl + "/locales/{{lng}}/{{ns}}.json",
  },
});

i18n.use(HttpBackend);
i18n.use(initReactI18next);
