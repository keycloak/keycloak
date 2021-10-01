import type ClientRepresentation from "@keycloak/keycloak-admin-client/lib/defs/clientRepresentation";
import type { TFunction } from "react-i18next";

/**
 * Checks if a client is intended to be used for authenticating a to a realm.
 */
export const isRealmClient = (client: ClientRepresentation) => !client.protocol;

/**
 * Gets a human readable name for the specified protocol.
 */
export const getProtocolName = (t: TFunction<"clients">, protocol: string) => {
  switch (protocol) {
    case "openid-connect":
      return t("clients:protocol:openIdConnect");
    case "saml":
      return t("clients:protocol:saml");
  }

  return protocol;
};
