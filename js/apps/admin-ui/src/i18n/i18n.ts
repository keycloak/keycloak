import { createInstance } from "i18next";
import type { i18n as i18nType } from "i18next";
import FetchBackend from "i18next-fetch-backend";
import { initReactI18next } from "react-i18next";

import { environment } from "../environment";
import { joinPath } from "../utils/joinPath";

// @ts-ignore imported by rollup plugin
import code from "message-bundle";

type KeyValue = { key: string; value: string };

export const DEFAULT_LOCALE = "en";
export const KEY_SEPARATOR = ".";

export const i18n: i18nType = createInstance({
  fallbackLng: DEFAULT_LOCALE,
  keySeparator: KEY_SEPARATOR,
  nsSeparator: false,
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
      if (
        process.env.NODE_ENV === "development" &&
        import.meta.env.VITE_REALM_OVERRIDES === undefined
      ) {
        return code;
      }
      const messages: KeyValue[] = JSON.parse(data);
      return Object.fromEntries(messages.map(({ key, value }) => [key, value]));
    },
  },
});

i18n.use(FetchBackend);
i18n.use(initReactI18next);
