import ResourceServerRepresentation from "@keycloak/keycloak-admin-client/lib/defs/resourceServerRepresentation";
import { useMemo, useState } from "react";
import { useAdminClient } from "../admin-client";
import { useFetch } from "@keycloak/keycloak-ui-shared";
import { sortBy } from "lodash-es";

type UseSortedResourceTypesProps = {
  clientId: string;
};

export default function useSortedResourceTypes({
  clientId,
}: UseSortedResourceTypesProps) {
  const { adminClient } = useAdminClient();
  const [resourceServer, setResourceServer] =
    useState<ResourceServerRepresentation>();

  useFetch(
    () =>
      adminClient.clients.getResourceServer({
        id: clientId,
      }),
    setResourceServer,
    [clientId],
  );

  const resourceTypes = useMemo(() => {
    const allResourceTypes = resourceServer?.authorizationSchema?.resourceTypes;
    return allResourceTypes
      ? sortBy(Object.values(allResourceTypes), "type")
      : [];
  }, [resourceServer]);

  return resourceTypes;
}
