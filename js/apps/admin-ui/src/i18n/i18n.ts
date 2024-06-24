import { createInstance } from "i18next";
import HttpBackend from "i18next-http-backend";
import { initReactI18next } from "react-i18next";

import { environment } from "../environment";
import { joinPath } from "../utils/joinPath";

type KeyValue = { key: string; value: string };

export const DEFAULT_LOCALE = "en";
export const KEY_SEPARATOR = ".";

export const i18n = createInstance({
  fallbackLng: DEFAULT_LOCALE,
  keySeparator: KEY_SEPARATOR,
  interpolation: {
    escapeValue: false,
  },
  defaultNS: [environment.realm],
  ns: [environment.realm],
  backend: {
    loadPath: joinPath(
      environment.adminBaseUrl,
      `resources/{{ns}}/admin/{{lng}}`,
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
i18n.use(initReactI18next);
