import { Spinner } from "@patternfly/react-core";
import Keycloak from "keycloak-js";
import {
  PropsWithChildren,
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useRef,
  useState,
} from "react";
import { AlertProvider } from "../alerts/Alerts";
import { stripForeignAuthResponse } from "../utils/stripForeignAuthResponse";
import { ErrorPage } from "./ErrorPage";
import { Help } from "./HelpContext";
import { BaseEnvironment } from "./environment";

const ACTION_UPDATE = "action-update";

export type ActionUpdate = {
  status: "success" | "cancelled" | "error";
  action?: string;
};

export type KeycloakContext<T extends BaseEnvironment = BaseEnvironment> =
  KeycloakContextProps<T> & {
    keycloak: Keycloak;
    /**
     * Listens for the result of the application-initiated action the user was redirected back
     * from. It is emitted once, after the provider has initialized and its children have mounted.
     * Returns a function that removes the listener.
     */
    subscribeActionUpdate: (
      listener: (update: ActionUpdate) => void,
    ) => () => void;
  };

const createKeycloakEnvContext = <T extends BaseEnvironment>() =>
  createContext<KeycloakContext<T> | undefined>(undefined);

let KeycloakEnvContext: any;

export const useEnvironment = <
  T extends BaseEnvironment = BaseEnvironment,
>() => {
  const context = useContext<KeycloakContext<T> | undefined>(
    KeycloakEnvContext,
  );
  if (!context)
    throw Error(
      "no environment provider in the hierarchy make sure to add the provider",
    );
  return context;
};

interface KeycloakContextProps<T extends BaseEnvironment> {
  environment: T;
  keycloak?: Keycloak;
}

export const KeycloakProvider = <T extends BaseEnvironment>({
  environment,
  keycloak: externalKeycloak,
  children,
}: PropsWithChildren<KeycloakContextProps<T>>) => {
  KeycloakEnvContext = createKeycloakEnvContext<T>();
  const calledOnce = useRef(false);
  const [init, setInit] = useState(!!externalKeycloak);
  const [error, setError] = useState<unknown>();
  const pendingActionUpdate = useRef<ActionUpdate>();
  const actionUpdates = useRef(new EventTarget());
  const subscribeActionUpdate = useCallback(
    (listener: (update: ActionUpdate) => void) => {
      const handler = (event: Event) =>
        listener((event as CustomEvent<ActionUpdate>).detail);
      actionUpdates.current.addEventListener(ACTION_UPDATE, handler);
      return () =>
        actionUpdates.current.removeEventListener(ACTION_UPDATE, handler);
    },
    [],
  );
  const keycloak = useMemo(() => {
    if (externalKeycloak) {
      return externalKeycloak;
    }
    const keycloak = new Keycloak({
      url: environment.serverBaseUrl,
      realm: environment.realm,
      clientId: environment.clientId,
    });

    keycloak.onAuthLogout = () => keycloak.login();

    return keycloak;
  }, [environment, externalKeycloak]);

  useEffect(() => {
    // Skip initialization if using external keycloak (already initialized)
    if (externalKeycloak) {
      return;
    }

    // only needed in dev mode
    if (calledOnce.current) {
      return;
    }

    // Discard an authorization response that was not requested by this application before the
    // adapter reads the current URL, so that it cannot end up in the `redirect_uri` of the login
    // we are about to start.
    const cleanedUrl = stripForeignAuthResponse(window.location.href);

    if (cleanedUrl) {
      window.history.replaceState(window.history.state, "", cleanedUrl);
    }

    keycloak.onActionUpdate = (status, action) => {
      pendingActionUpdate.current = { status, action };
    };

    const init = () =>
      keycloak.init({
        onLoad: "login-required",
        pkceMethod: "S256",
        responseMode: "query",
        scope: environment.scope,
      });

    init()
      .then(() => setInit(true))
      .catch((error) => setError(error));

    calledOnce.current = true;
  }, [keycloak, externalKeycloak]);

  // Children have mounted (and their effects ran) by the time this runs, so they can subscribe.
  useEffect(() => {
    if (!init || !pendingActionUpdate.current) {
      return;
    }

    const detail = pendingActionUpdate.current;
    pendingActionUpdate.current = undefined;
    actionUpdates.current.dispatchEvent(
      new CustomEvent(ACTION_UPDATE, { detail }),
    );
  }, [init]);

  if (error) {
    return <ErrorPage error={error} />;
  }

  if (!init) {
    return <Spinner />;
  }

  return (
    <KeycloakEnvContext.Provider
      value={{
        environment,
        keycloak,
        subscribeActionUpdate,
      }}
    >
      <AlertProvider>
        <Help>{children}</Help>
      </AlertProvider>
    </KeycloakEnvContext.Provider>
  );
};
