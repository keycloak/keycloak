import React, { useState } from "react";
import { DescriptionList } from "@patternfly/react-core";

import type ResourceServerRepresentation from "@keycloak/keycloak-admin-client/lib/defs/resourceServerRepresentation";
import { KeycloakSpinner } from "../../components/keycloak-spinner/KeycloakSpinner";
import { useAdminClient, useFetch } from "../../context/auth/AdminClient";
import { DetailDescription } from "./DetailDescription";

import "./detail-cell.css";

type Scope = { id: string; name: string }[];

type DetailCellProps = {
  id: string;
  clientId: string;
  uris?: string[];
};

export const DetailCell = ({ id, clientId, uris }: DetailCellProps) => {
  const adminClient = useAdminClient();
  const [scope, setScope] = useState<Scope>();
  const [permissions, setPermissions] =
    useState<ResourceServerRepresentation[]>();

  useFetch(
    () =>
      Promise.all([
        adminClient.clients.listScopesByResource({
          id: clientId,
          resourceName: id,
        }),
        adminClient.clients.listPermissionsByResource({
          id: clientId,
          resourceId: id,
        }),
      ]),
    ([scopes, permissions]) => {
      setScope(scopes);
      setPermissions(permissions);
    },
    []
  );

  if (!permissions || !scope) {
    return <KeycloakSpinner />;
  }

  return (
    <DescriptionList isHorizontal className="keycloak_resource_details">
      <DetailDescription name="uris" array={uris} />
      <DetailDescription name="scopes" array={scope} convert={(s) => s.name} />
      <DetailDescription
        name="associatedPermissions"
        array={permissions}
        convert={(p) => p.name!}
      />
    </DescriptionList>
  );
};
