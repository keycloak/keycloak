import React, { useContext, useEffect, useState } from "react";
import { useErrorHandler } from "react-error-boundary";
import i18n from "../../i18n";

import { AdminClient, asyncStateFetch } from "../auth/AdminClient";
import { RealmContext } from "../realm-context/RealmContext";
import WhoAmIRepresentation, {
  AccessType,
} from "keycloak-admin/lib/defs/whoAmIRepresentation";

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
  const adminClient = useContext(AdminClient)!;
  const handleError = useErrorHandler();
  const { realm, setRealm } = useContext(RealmContext);
  const [whoAmI, setWhoAmI] = useState<WhoAmI>(new WhoAmI());
  const [key, setKey] = useState(0);

  useEffect(() => {
    return asyncStateFetch(
      () =>
        adminClient.whoAmI.find({
          realm: adminClient.keycloak?.realm,
        }),
      (me) => {
        const whoAmI = new WhoAmI(adminClient.keycloak?.realm, me);
        if (!realm) {
          setRealm(whoAmI.getHomeRealm());
        }
        setWhoAmI(whoAmI);
      },
      handleError
    );
  }, [key]);

  return (
    <WhoAmIContext.Provider value={{ refresh: () => setKey(key + 1), whoAmI }}>
      {children}
    </WhoAmIContext.Provider>
  );
};
