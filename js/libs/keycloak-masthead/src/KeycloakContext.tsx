import Keycloak, { type KeycloakTokenParsed } from "keycloak-js";
import {
  createContext,
  PropsWithChildren,
  useContext,
  useMemo,
  useState,
} from "react";

type KeycloakProps = {
  keycloak: Keycloak;
  token?: KeycloakTokenParsed;
  updateToken: () => void;
};

const KeycloakContext = createContext<KeycloakProps | undefined>(undefined);

export type KeycloakProviderProps = {
  keycloak: Keycloak;
};

export const useKeycloak = () => useContext(KeycloakContext);

export const KeycloakProvider = ({
  keycloak,
  children,
}: PropsWithChildren<KeycloakProviderProps>) => {
  const [token, setToken] = useState(keycloak.tokenParsed);
  const context = useMemo(
    () => ({
      keycloak,
      token,
      updateToken: async () => {
        await keycloak.updateToken(-1);
        setToken(keycloak.tokenParsed);
      },
    }),
    [keycloak, token],
  );
  return (
    <KeycloakContext.Provider value={context}>
      {children}
    </KeycloakContext.Provider>
  );
};
