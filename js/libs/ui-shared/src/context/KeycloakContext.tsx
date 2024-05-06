import { Spinner } from "@patternfly/react-core";
import Keycloak from "keycloak-js";
import {
  PropsWithChildren,
  createContext,
  useContext,
  useEffect,
  useMemo,
  useRef,
  useState,
} from "react";
import { AlertProvider } from "../alerts/Alerts";
import { ErrorPage } from "./ErrorPage";
import { Help } from "./HelpContext";
import { AccountEnvironment, AdminEnvironment } from "./environment";

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
  environment: AdminEnvironment | AccountEnvironment;
};

export const KeycloakProvider = ({
  environment,
  children,
}: PropsWithChildren<KeycloakContextProps>) => {
  const calledOnce = useRef(false);
  const [init, setInit] = useState(false);
  const [error, setError] = useState<unknown>();
  const keycloak = useMemo(() => {
    const keycloak = new Keycloak({
      url: environment.authUrl,
      realm: environment.realm,
      clientId: environment.clientId,
    });

    keycloak.onAuthLogout = () => keycloak.login();

    return keycloak;
  }, [environment]);

  useEffect(() => {
    // only needed in dev mode
    if (calledOnce.current) {
      return;
    }

    const init = () =>
      keycloak.init({
        onLoad: "check-sso",
        pkceMethod: "S256",
        responseMode: "query",
      });

    init()
      .then(() => setInit(true))
      .catch((error) => setError(error));

    calledOnce.current = true;
  }, [keycloak]);

  if (error) {
    return <ErrorPage error={error} />;
  }

  if (!init) {
    return <Spinner />;
  }

  return (
    <KeycloakEnvContext.Provider value={{ environment, keycloak }}>
      <AlertProvider>
        <Help>{children}</Help>
      </AlertProvider>
    </KeycloakEnvContext.Provider>
  );
};
