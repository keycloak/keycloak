import React, { useState } from "react";
import i18n from "../../i18n";

import type WhoAmIRepresentation from "keycloak-admin/lib/defs/whoAmIRepresentation";
import type { AccessType } from "keycloak-admin/lib/defs/whoAmIRepresentation";
import { useAdminClient, useFetch } from "../auth/AdminClient";

export class WhoAmI {
  constructor(
    private homeRealm?: string | undefined,
    private me?: WhoAmIRepresentation | undefined
  ) {
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

  /**
   * Return the realm I am signed in to.
   */
  public getHomeRealm(): string {
    let realm: string | undefined = this.homeRealm;
    if (realm === undefined) realm = this.me?.realm;
    if (realm === undefined) realm = "master"; // this really can't happen in the real world

    return realm;
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

export const WhoAmIContext = React.createContext<WhoAmIProps>({
  refresh: () => {},
  whoAmI: new WhoAmI(),
});

type WhoAmIProviderProps = { children: React.ReactNode };
export const WhoAmIContextProvider = ({ children }: WhoAmIProviderProps) => {
  const adminClient = useAdminClient();
  const [whoAmI, setWhoAmI] = useState<WhoAmI>(new WhoAmI());
  const [key, setKey] = useState(0);

  useFetch(
    () => adminClient.whoAmI.find(),
    (me) => {
      const whoAmI = new WhoAmI(adminClient.keycloak?.realm, me);
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
