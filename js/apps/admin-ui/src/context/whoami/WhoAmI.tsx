import type WhoAmIRepresentation from "@keycloak/keycloak-admin-client/lib/defs/whoAmIRepresentation";
import type { AccessType } from "@keycloak/keycloak-admin-client/lib/defs/whoAmIRepresentation";
import {
  createNamedContext,
  useEnvironment,
  useFetch,
  useRequiredContext,
} from "@keycloak/keycloak-ui-shared";
import { PropsWithChildren, useState } from "react";
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

export class WhoAmI {
  #me?: WhoAmIRepresentation;

  constructor(me?: WhoAmIRepresentation) {
    this.#me = me;
    if (this.#me?.locale) {
      i18n.changeLanguage(this.#me.locale, (error) => {
        if (error) {
          console.warn("Error(s) loading locale", this.#me?.locale, error);
        }
      });
      if (RTL_LOCALES.includes(this.#me.locale)) {
        document.getElementsByTagName("html")[0].setAttribute("dir", "rtl");
      }
    }
  }

  public getDisplayName(): string {
    if (this.#me === undefined) return "";

    return this.#me.displayName;
  }

  public getLocale() {
    return this.#me?.locale ?? DEFAULT_LOCALE;
  }

  public getRealm() {
    return this.#me?.realm ?? "";
  }

  public getUserId(): string {
    if (this.#me === undefined) return "";

    return this.#me.userId;
  }

  public canCreateRealm(): boolean {
    return !!this.#me?.createRealm;
  }

  public getRealmAccess(): Readonly<{
    [key: string]: ReadonlyArray<AccessType>;
  }> {
    if (this.#me === undefined) return {};

    return this.#me.realm_access;
  }

  public isTemporary(): boolean {
    return this.#me?.temporary ?? false;
  }
}

type WhoAmIProps = {
  refresh: () => void;
  whoAmI: WhoAmI;
};

export const WhoAmIContext = createNamedContext<WhoAmIProps | undefined>(
  "WhoAmIContext",
  undefined,
);

export const useWhoAmI = () => useRequiredContext(WhoAmIContext);

export const WhoAmIContextProvider = ({ children }: PropsWithChildren) => {
  const { adminClient } = useAdminClient();
  const { environment } = useEnvironment();

  const [whoAmI, setWhoAmI] = useState<WhoAmI>(new WhoAmI());
  const { realm } = useRealm();
  const [key, setKey] = useState(0);

  useFetch(
    () =>
      adminClient.whoAmI.find({
        realm: environment.realm,
        currentRealm: realm!,
      }),
    (me) => {
      const whoAmI = new WhoAmI(me);
      setWhoAmI(whoAmI);
    },
    [key, realm],
  );

  return (
    <WhoAmIContext.Provider value={{ refresh: () => setKey(key + 1), whoAmI }}>
      {children}
    </WhoAmIContext.Provider>
  );
};
