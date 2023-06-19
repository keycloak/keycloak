import { createInstance } from "i18next";
import { initReactI18next } from "react-i18next";

import environment from "../environment";
import { joinPath } from "../utils/joinPath";
import { OverridesBackend } from "./OverridesBackend";

export const DEFAULT_LOCALE = "en";
export const DEFAULT_NAMESPACE = "common";
export const NAMESPACE_SEPARATOR = ":";
export const KEY_SEPARATOR = ".";

export const i18n = createInstance({
  fallbackLng: DEFAULT_LOCALE,
  defaultNS: DEFAULT_NAMESPACE,
  nsSeparator: NAMESPACE_SEPARATOR,
  keySeparator: KEY_SEPARATOR,
  ns: [
    DEFAULT_NAMESPACE,
    "common-help",
    "dashboard",
    "clients",
    "clients-help",
    "client-scopes",
    "client-scopes-help",
    "groups",
    "realm",
    "roles",
    "users",
    "users-help",
    "sessions",
    "events",
    "realm-settings",
    "realm-settings-help",
    "authentication",
    "authentication-help",
    "user-federation",
    "user-federation-help",
    "identity-providers",
    "identity-providers-help",
    "dynamic",
  ],
  interpolation: {
    escapeValue: false,
  },
  backend: {
    loadPath: joinPath(environment.resourceUrl, "locales/{{lng}}/{{ns}}.json"),
  },
});

i18n.use(OverridesBackend);
i18n.use(initReactI18next);
