import type ClientRepresentation from "@keycloak/keycloak-admin-client/lib/defs/clientRepresentation";

/**
 * Checks if a client is intended to be used for authenticating a to a realm.
 */
export const isRealmClient = (client: ClientRepresentation) => !client.protocol;
