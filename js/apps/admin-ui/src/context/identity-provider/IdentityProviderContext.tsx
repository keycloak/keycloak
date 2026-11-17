import type IdentityProviderRepresentation from "@keycloak/keycloak-admin-client/lib/defs/identityProviderRepresentation";
import { createNamedContext } from "@keycloak/keycloak-ui-shared";
import { PropsWithChildren, useContext } from "react";

export const IdentityProviderContext = createNamedContext<
  IdentityProviderRepresentation | undefined
>("IdentityProviderContext", undefined);

/**
 * The identity provider a mapper form belongs to, or undefined outside of one.
 */
export const useIdentityProvider = () => useContext(IdentityProviderContext);

type IdentityProviderContextProviderProps = PropsWithChildren & {
  value?: IdentityProviderRepresentation;
};

export const IdentityProviderContextProvider = ({
  value,
  children,
}: IdentityProviderContextProviderProps) => (
  <IdentityProviderContext.Provider value={value}>
    {children}
  </IdentityProviderContext.Provider>
);
