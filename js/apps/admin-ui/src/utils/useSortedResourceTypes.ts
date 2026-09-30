import ResourceServerRepresentation from "@keycloak/keycloak-admin-client/lib/defs/resourceServerRepresentation";
import { useMemo, useState } from "react";
import { useAdminClient } from "../admin-client";
import { useRealm } from "../context/realm-context/RealmContext";
import useIsFeatureEnabled, { Feature } from "./useIsFeatureEnabled";
import { useFetch } from "@keycloak/keycloak-ui-shared";
import { sortBy } from "lodash-es";

type UseSortedResourceTypesProps = {
  clientId: string;
};

export default function useSortedResourceTypes({
  clientId,
}: UseSortedResourceTypesProps) {
  const { adminClient } = useAdminClient();
  const { realmRepresentation } = useRealm();
  const isFeatureEnabled = useIsFeatureEnabled();
  const organizationsAvailable =
    isFeatureEnabled(Feature.Organizations) &&
    realmRepresentation.organizationsEnabled;
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
      ? sortBy(
          Object.values(allResourceTypes).filter(
            ({ type }) => type !== "Organizations" || organizationsAvailable,
          ),
          "type",
        )
      : [];
  }, [resourceServer, organizationsAvailable]);

  return resourceTypes;
}
