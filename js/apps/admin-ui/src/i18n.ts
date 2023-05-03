import { InitOptions, TOptions, init, use } from "i18next";
import HttpBackend, { LoadPathOption } from "i18next-http-backend";
import { initReactI18next } from "react-i18next";

import { adminClient } from "./admin-client";
import environment from "./environment";
import { addTrailingSlash } from "./util";
import { getAuthorizationHeaders } from "./utils/getAuthorizationHeaders";

export const DEFAULT_LOCALE = "en";

export async function initI18n() {
  const options = await initOptions();
  await init(options);
}

const initOptions = async (): Promise<InitOptions> => {
  const constructLoadPath: LoadPathOption = (_, namespaces) => {
    if (namespaces[0] === "overrides") {
      return `${addTrailingSlash(adminClient.baseUrl)}admin/realms/${
        adminClient.realmName
      }/localization/{{lng}}?useRealmDefaultLocaleFallback=true`;
    } else {
      return `${environment.resourceUrl}/locales/{{lng}}/{{ns}}.json`;
    }
  };

  return {
    returnNull: false,
    defaultNS: "common",
    fallbackLng: DEFAULT_LOCALE,
    preload: [DEFAULT_LOCALE],
    ns: [
      "common",
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
      "overrides",
    ],
    interpolation: {
      escapeValue: false,
    },
    postProcess: ["overrideProcessor"],
    backend: {
      loadPath: constructLoadPath,
      customHeaders: getAuthorizationHeaders(
        await adminClient.getAccessToken()
      ),
    },
  };
};

const configuredI18n = use({
  type: "postProcessor",
  name: "overrideProcessor",
  process: function (value: string, key: string, _: TOptions, translator: any) {
    const override: string =
      translator.resourceStore.data[translator.language].overrides?.[key];
    return override || value;
  },
})
  .use(initReactI18next)
  .use(HttpBackend);

export default configuredI18n;
