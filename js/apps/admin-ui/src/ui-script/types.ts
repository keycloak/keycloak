import type RealmRepresentation from "@keycloak/keycloak-admin-client/lib/defs/realmRepresentation";

export type UiScriptContext = {
  realm: string;
  realmRepresentation: RealmRepresentation;
  adminBaseUrl: string;
  serverBaseUrl: string;
  authServerUrl: string;
  getAccessToken: () => Promise<string | undefined>;
};

export type UiScriptElement = HTMLElement & {
  context?: UiScriptContext;
};
