import { createContext, useContext } from "react";
import KeycloakAdminClient from "keycloak-admin";
import { RealmContext } from "../realm-context/RealmContext";

export const AdminClient = createContext<KeycloakAdminClient | undefined>(
  undefined
);

export const useAdminClient = () => {
  const adminClient = useContext(AdminClient)!;
  const { realm } = useContext(RealmContext);

  adminClient.setConfig({
    realmName: realm,
  });

  return adminClient;
};
