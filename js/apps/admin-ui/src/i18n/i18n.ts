import { createInstance } from "i18next";
import { initReactI18next } from "react-i18next";

import environment from "../environment";
import { joinPath } from "../utils/joinPath";
import { OverridesBackend } from "./OverridesBackend";

export const DEFAULT_LOCALE = "en";
export const DEFAULT_NAMESPACE = "translation";
export const KEY_SEPARATOR = ".";

export const i18n = createInstance({
  fallbackLng: DEFAULT_LOCALE,
  defaultNS: DEFAULT_NAMESPACE,
  fallbackNS: DEFAULT_NAMESPACE,
  keySeparator: KEY_SEPARATOR,
  ns: DEFAULT_NAMESPACE,
  interpolation: {
    escapeValue: false,
  },
  backend: {
    loadPath: joinPath(environment.resourceUrl, `locales/{{lng}}/{{ns}}.json`),
  },
});

i18n.use(OverridesBackend);
i18n.use(initReactI18next);
