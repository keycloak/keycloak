import type WhoAmIRepresentation from "keycloak-admin/lib/defs/whoAmIRepresentation";
import type { AccessType } from "keycloak-admin/lib/defs/whoAmIRepresentation";
import React, { useState } from "react";
import i18n from "../../i18n";
import useRequiredContext from "../../utils/useRequiredContext";
import { useAdminClient, useFetch } from "../auth/AdminClient";

export class WhoAmI {
  constructor(private me?: WhoAmIRepresentation) {
    if (this.me !== undefined && this.me.locale) {
      i18n.changeLanguage(this.me.locale, (error) => {
        if (error) console.error("Unable to set locale to", this.me?.locale);
      });
    }
  }

  public getDisplayName(): string {
    if (this.me === undefined) return "";

    return this.me.displayName;
  }

  public getUserId(): string {
    if (this.me === undefined) return "";

    return this.me.userId;
  }

  public canCreateRealm(): boolean {
    return this.me !== undefined && this.me.createRealm;
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

export const WhoAmIContext = React.createContext<WhoAmIProps | undefined>(
  undefined
);

export const useWhoAmI = () => useRequiredContext(WhoAmIContext);

type WhoAmIProviderProps = { children: React.ReactNode };
export const WhoAmIContextProvider = ({ children }: WhoAmIProviderProps) => {
  const adminClient = useAdminClient();
  const [whoAmI, setWhoAmI] = useState<WhoAmI>(new WhoAmI());
  const [key, setKey] = useState(0);

  useFetch(
    () => adminClient.whoAmI.find(),
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
