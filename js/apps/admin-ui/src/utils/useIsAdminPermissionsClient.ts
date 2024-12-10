import { useState } from "react";
import { useFetch } from "@keycloak/keycloak-ui-shared";
import { useAdminClient } from "../admin-client";
import ClientRepresentation from "@keycloak/keycloak-admin-client/lib/defs/clientRepresentation";

export function useIsAdminPermissionsClient(selectedClientId: string) {
  const { adminClient } = useAdminClient();
  const [isAdminPermissionsClient, setIsAdminPermissionsClient] =
    useState<boolean>(false);

  useFetch(
    async () => {
      const clients: ClientRepresentation[] = await adminClient.clients.find();
      return clients;
    },
    (clients: ClientRepresentation[]) => {
      const adminPermissionsClient = clients.find(
        (client) => client.clientId === "admin-permissions",
      );

      setIsAdminPermissionsClient(
        selectedClientId === adminPermissionsClient?.id,
      );
    },
    [],
  );

  return isAdminPermissionsClient;
}
