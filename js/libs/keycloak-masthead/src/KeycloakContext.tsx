import Keycloak, { KeycloakTokenParsed } from "keycloak-js";
import { createContext, PropsWithChildren, useContext, useState } from "react";

type KeycloakProps = {
  keycloak?: Keycloak;
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
  const updateToken = async () => {
    await keycloak.updateToken(-1);
    setToken(keycloak.tokenParsed);
  };
  console.log("what", keycloak, token, updateToken);
  return (
    <KeycloakContext.Provider value={{ keycloak, token, updateToken }}>
      {children}
    </KeycloakContext.Provider>
  );
};
