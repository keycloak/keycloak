import KeycloakAdminClient from "@keycloak/keycloak-admin-client";
import { fetchAdminUI } from "../../context/auth/admin-ui-endpoint";

export const getUnmanagedAttributes = (
  adminClient: KeycloakAdminClient,
  id: string,
): Promise<Record<string, string[]> | undefined> =>
  fetchAdminUI(adminClient, `ui-ext/users/${id}/unmanagedAttributes`);
