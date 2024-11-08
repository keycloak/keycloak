import type ClientRepresentation from "@keycloak/keycloak-admin-client/lib/defs/clientRepresentation";
import { useFetch } from "@keycloak/keycloak-ui-shared";
import { useState } from "react";
import { useAdminClient } from "../admin-client";
import { ViewHeader } from "../components/view-header/ViewHeader";
import { PermissionsResources } from "./PermissionsResources";

export default function PermissionsSection() {
  const { adminClient } = useAdminClient();
  const [realmManagementClient, setRealmManagementClient] = useState<
    ClientRepresentation | undefined
  >();

  useFetch(
    async () => {
      const clients = await adminClient.clients.find();
      return clients;
    },
    (clients) => {
      const realmManagementClient = clients.find(
        (client) => client.clientId === "realm-management",
      );
      setRealmManagementClient(realmManagementClient!);
    },
    [],
  );

  return (
    realmManagementClient && (
      <>
        <ViewHeader titleKey={realmManagementClient.clientId!} />
        <PermissionsResources clientId={realmManagementClient.id!} />
      </>
    )
  );
}
