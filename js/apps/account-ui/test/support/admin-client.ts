import AdminClient from "@keycloak/keycloak-admin-client";
import { ADMIN_PASSWORD, ADMIN_USERNAME, SERVER_URL } from "./common.ts";

export const adminClient = new AdminClient({
  baseUrl: SERVER_URL.toString(),
});

await adminClient.auth({
  username: ADMIN_USERNAME,
  password: ADMIN_PASSWORD,
  grantType: "password",
  clientId: "admin-cli",
});
