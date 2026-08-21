import ResourceServerRepresentation from "@keycloak/keycloak-admin-client/lib/defs/resourceServerRepresentation";
import { useMemo, useState } from "react";
import { useAdminClient } from "../admin-client";
import { useFetch } from "@keycloak/keycloak-ui-shared";
import { sortBy } from "lodash-es";
import useIsFeatureEnabled, { Feature } from "./useIsFeatureEnabled";
import { useRealm } from "../context/realm-context/RealmContext";

type UseSortedResourceTypesProps = {
  clientId: string;
};

export default function useSortedResourceTypes({
  clientId,
}: UseSortedResourceTypesProps) {
  const { adminClient } = useAdminClient();
  const isFeatureEnabled = useIsFeatureEnabled();
  const { realmRepresentation: realm } = useRealm();
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
    const orgsEnabled =
      isFeatureEnabled(Feature.Organizations) && realm.organizationsEnabled;

    const allResourceTypes = resourceServer?.authorizationSchema?.resourceTypes;

    return allResourceTypes
      ? sortBy(Object.values(allResourceTypes), "type").filter(
          ({ type }) => type !== "Organizations" || orgsEnabled,
        )
      : [];
  }, [resourceServer, isFeatureEnabled, realm]);

  return resourceTypes;
}
