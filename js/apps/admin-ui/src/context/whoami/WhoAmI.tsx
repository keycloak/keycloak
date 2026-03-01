import type WhoAmIRepresentation from "@keycloak/keycloak-admin-client/lib/defs/whoAmIRepresentation";
import {
  createNamedContext,
  KeycloakSpinner,
  useEnvironment,
  useFetch,
  useRequiredContext,
} from "@keycloak/keycloak-ui-shared";
import { PropsWithChildren, useEffect, useState } from "react";
import { useAdminClient } from "../../admin-client";
import { DEFAULT_LOCALE, i18n } from "../../i18n/i18n";
import { useRealm } from "../realm-context/RealmContext";

// can be replaced with https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Intl/Locale/getTextInfo
const RTL_LOCALES = [
  "ar",
  "dv",
  "fa",
  "ha",
  "he",
  "iw",
  "ji",
  "ps",
  "sd",
  "ug",
  "ur",
  "yi",
];

type WhoAmIProps = {
  refresh: () => void;
  whoAmI: WhoAmIRepresentation;
};

export const WhoAmIContext = createNamedContext<WhoAmIProps | undefined>(
  "WhoAmIContext",
  undefined,
);

export const useWhoAmI = () => useRequiredContext(WhoAmIContext);

export const WhoAmIContextProvider = ({ children }: PropsWithChildren) => {
  const { adminClient } = useAdminClient();
  const { environment } = useEnvironment();

  const [whoAmI, setWhoAmI] = useState<WhoAmIRepresentation>();
  const { realm } = useRealm();
  const [key, setKey] = useState(0);

  useFetch(
    async () => {
      try {
        return await adminClient.whoAmI.find({
          realm: environment.realm,
          currentRealm: realm,
        });
      } catch (error) {
        console.warn(
          "Unable to fetch whoami, falling back to empty defaults.",
          error,
        );

        return {
          realm: "",
          userId: "",
          displayName: "",
          locale: DEFAULT_LOCALE,
          createRealm: false,
          realm_access: {},
          temporary: false,
        };
      }
    },
    setWhoAmI,
    [key, environment.realm, realm],
  );

  useFetch(
    async () => {
      if (whoAmI?.locale) {
        await i18n.changeLanguage(whoAmI.locale);
      }
    },
    // eslint-disable-next-line @typescript-eslint/no-empty-function
    () => {}, // noop
    [whoAmI?.locale],
  );

  useEffect(() => {
    if (whoAmI?.locale && RTL_LOCALES.includes(whoAmI.locale)) {
      document.documentElement.setAttribute("dir", "rtl");
    } else {
      document.documentElement.removeAttribute("dir");
    }
  }, [whoAmI?.locale]);

  if (!whoAmI) {
    return <KeycloakSpinner />;
  }

  return (
    <WhoAmIContext.Provider value={{ refresh: () => setKey(key + 1), whoAmI }}>
      {children}
    </WhoAmIContext.Provider>
  );
};
