import React, { useContext } from "react";
import i18n from "../../i18n";

import WhoAmIRepresentation from "./who-am-i-model";

import { HttpClientContext } from "../http-service/HttpClientContext";
import { KeycloakContext } from "../auth/KeycloakContext";
import { DataLoader } from "../../components/data-loader/DataLoader";

export class WhoAmI {
  constructor(
    private homeRealm?: string | undefined,
    private me?: WhoAmIRepresentation | undefined
  ) {
    if (this.me !== undefined && this.me.locale) {
      i18n.changeLanguage(this.me.locale, (error) => {
        if (error) console.log("Unable to set locale to " + this.me?.locale);
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

  public getRealmAccess(): Readonly<{ [key: string]: ReadonlyArray<string> }> {
    if (this.me === undefined) return {};

    return this.me.realm_access;
  }
}

export const WhoAmIContext = React.createContext(new WhoAmI());

type WhoAmIProviderProps = { children: React.ReactNode };
export const WhoAmIContextProvider = ({ children }: WhoAmIProviderProps) => {
  const httpClient = useContext(HttpClientContext)!;
  const keycloak = useContext(KeycloakContext);

  const whoAmILoader = async () => {
    if (keycloak === undefined) return undefined;

    const realm = keycloak.realm();

    return await httpClient
      .doGet(`/admin/${realm}/console/whoami/`)
      .then((r) => r.data as WhoAmIRepresentation);
  };

  return (
    <DataLoader loader={whoAmILoader}>
      {(whoamirep) => (
        <WhoAmIContext.Provider
          value={new WhoAmI(keycloak?.realm(), whoamirep)}
        >
          {children}
        </WhoAmIContext.Provider>
      )}
    </DataLoader>
  );
};
