import AdminClient from "@keycloak/keycloak-admin-client";
import type UserRepresentation from "@keycloak/keycloak-admin-client/lib/defs/userRepresentation.js";
import { ADMIN_PASSWORD, ADMIN_USERNAME, SERVER_URL } from "./common.ts";

export const adminClient = new AdminClient({
  baseUrl: SERVER_URL,
});

await adminClient.auth({
  username: ADMIN_USERNAME,
  password: ADMIN_PASSWORD,
  grantType: "password",
  clientId: "admin-cli",
});

export async function findUserByUsername(
  realm: string,
  username: string,
): Promise<UserRepresentation> {
  const users = await adminClient.users.find({
    realm,
    username,
    exact: true,
    max: 1,
  });

  return users[0];
}
