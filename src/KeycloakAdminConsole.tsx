import React from "react";
import KeycloakAdminClient from "keycloak-admin";

import { AdminClient } from "./context/auth/AdminClient";
import { WhoAmIContextProvider } from "./context/whoami/WhoAmI";
import { RealmContextProvider } from "./context/realm-context/RealmContext";
import { App } from "./App";

export type KeycloakAdminConsoleProps = {
  adminClient: KeycloakAdminClient;
};

export const KeycloakAdminConsole = ({
  adminClient,
}: KeycloakAdminConsoleProps) => {
  return (
    <RealmContextProvider>
      <AdminClient.Provider value={adminClient}>
        <WhoAmIContextProvider>
          <App />
        </WhoAmIContextProvider>
      </AdminClient.Provider>
    </RealmContextProvider>
  );
};
