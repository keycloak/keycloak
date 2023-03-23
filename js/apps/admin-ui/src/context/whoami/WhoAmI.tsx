import type WhoAmIRepresentation from "@keycloak/keycloak-admin-client/lib/defs/whoAmIRepresentation";
import type { AccessType } from "@keycloak/keycloak-admin-client/lib/defs/whoAmIRepresentation";
import { PropsWithChildren, useState } from "react";

import environment from "../../environment";
import i18n, { DEFAULT_LOCALE } from "../../i18n";
import { createNamedContext, useRequiredContext } from "ui-shared";
import { useAdminClient, useFetch } from "../auth/AdminClient";

export class WhoAmI {
  constructor(private me?: WhoAmIRepresentation) {
    if (this.me?.locale) {
      i18n.changeLanguage(this.me.locale, (error) => {
        if (error) console.error("Unable to set locale to", this.me?.locale);
      });
    }
  }

  public getDisplayName(): string {
    if (this.me === undefined) return "";

    return this.me.displayName;
  }

  public getLocale() {
    return this.me?.locale ?? DEFAULT_LOCALE;
  }

  public getRealm() {
    return this.me?.realm ?? "";
  }

  public getUserId(): string {
    if (this.me === undefined) return "";

    return this.me.userId;
  }

  public canCreateRealm(): boolean {
    return !!this.me?.createRealm;
  }

  public getRealmAccess(): Readonly<{
    [key: string]: ReadonlyArray<AccessType>;
  }> {
    if (this.me === undefined) return {};

    return this.me.realm_access;
  }
}

type WhoAmIProps = {
  refresh: () => void;
  whoAmI: WhoAmI;
};

export const WhoAmIContext = createNamedContext<WhoAmIProps | undefined>(
  "WhoAmIContext",
  undefined
);

export const useWhoAmI = () => useRequiredContext(WhoAmIContext);

export const WhoAmIContextProvider = ({ children }: PropsWithChildren) => {
  const { adminClient } = useAdminClient();
  const [whoAmI, setWhoAmI] = useState<WhoAmI>(new WhoAmI());
  const [key, setKey] = useState(0);

  useFetch(
    () => adminClient.whoAmI.find({ realm: environment.loginRealm }),
    (me) => {
      const whoAmI = new WhoAmI(me);
      setWhoAmI(whoAmI);
    },
    [key]
  );

  return (
    <WhoAmIContext.Provider value={{ refresh: () => setKey(key + 1), whoAmI }}>
      {children}
    </WhoAmIContext.Provider>
  );
};
