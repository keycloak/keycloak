import type ClientRepresentation from "@keycloak/keycloak-admin-client/lib/defs/clientRepresentation";
import type { TFunction } from "i18next";

/**
 * Checks if a client is intended to be used for authenticating a to a realm.
 */
export const isRealmClient = (client: ClientRepresentation): boolean =>
  client.attributes?.["realm_client"] === true.toString();

/**
 * Gets a human readable name for the specified protocol.
 */
export const getProtocolName = (t: TFunction<"clients">, protocol: string) => {
  switch (protocol) {
    case "openid-connect":
      return t("protocolTypes.openid-connect");
    case "saml":
      return t("protocolTypes.saml");
    case "oid4vc":
      return t("protocolTypes.oid4vc");
    default:
      return protocol;
  }

  return protocol;
};

export const defaultContextAttributes = [
  {
    key: "custom",
    name: "Custom Attribute...",
    custom: true,
  },
  {
    key: "kc.identity.authc.method",
    name: "Authentication Method",
    values: [
      {
        key: "pwd",
        name: "Password",
      },
      {
        key: "otp",
        name: "One-Time Password",
      },
      {
        key: "kbr",
        name: "Kerberos",
      },
    ],
  },
  {
    key: "kc.realm.name",
    name: "Realm",
  },
  {
    key: "kc.time.date_time",
    name: "Date/Time (MM/dd/yyyy hh:mm:ss)",
  },
  {
    key: "kc.client.network.ip_address",
    name: "Client IPv4 Address",
  },
  {
    key: "kc.client.network.host",
    name: "Client Host",
  },
  {
    key: "kc.client.user_agent",
    name: "Client/User Agent",
  },
];
