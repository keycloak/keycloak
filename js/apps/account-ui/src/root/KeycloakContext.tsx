import Keycloak from "keycloak-js";
import {
  PropsWithChildren,
  createContext,
  useContext,
  useEffect,
  useMemo,
} from "react";
import { Environment } from "../environment";
import { AlertProvider, Help } from "ui-shared";

export type KeycloakContext = KeycloakContextProps & {
  keycloak: Keycloak;
};

const KeycloakEnvContext = createContext<KeycloakContext | undefined>(
  undefined,
);

export const useEnvironment = () => {
  const context = useContext(KeycloakEnvContext);
  if (!context)
    throw Error(
      "no environment provider in the hierarchy make sure to add the provider",
    );
  return context;
};

type KeycloakContextProps = {
  environment: Environment;
};

export const KeycloakProvider = ({
  environment,
  children,
}: PropsWithChildren<KeycloakContextProps>) => {
  const keycloak = useMemo(
    () =>
      new Keycloak({
        url: environment.authUrl,
        realm: environment.realm,
        clientId: environment.clientId,
      }),
    [environment],
  );

  useEffect(() => {
    (() =>
      keycloak.init({
        onLoad: "check-sso",
        pkceMethod: "S256",
      }))();
  }, [keycloak]);

  return (
    <KeycloakEnvContext.Provider value={{ environment, keycloak }}>
      <AlertProvider>
        <Help>{children}</Help>
      </AlertProvider>
    </KeycloakEnvContext.Provider>
  );
};
