import type ResourceServerRepresentation from "@keycloak/keycloak-admin-client/lib/defs/resourceServerRepresentation";
import { useFetch } from "@keycloak/keycloak-ui-shared";
import { DescriptionList } from "@patternfly/react-core";
import { useState } from "react";
import { useAdminClient } from "../../admin-client";
import { KeycloakSpinner } from "@keycloak/keycloak-ui-shared";
import { useRealm } from "../../context/realm-context/RealmContext";
import { toPermissionDetails } from "../routes/PermissionDetails";
import { toScopeDetails } from "../routes/Scope";
import { DetailDescription, DetailDescriptionLink } from "./DetailDescription";

import "./detail-cell.css";

type Scope = { id: string; name: string }[];

type DetailCellProps = {
  id: string;
  clientId: string;
  uris?: string[];
};

export const DetailCell = ({ id, clientId, uris }: DetailCellProps) => {
  const { adminClient } = useAdminClient();

  const { realm } = useRealm();
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
    [],
  );

  if (!permissions || !scope) {
    return <KeycloakSpinner />;
  }

  return (
    <DescriptionList isHorizontal className="keycloak_resource_details">
      <DetailDescription name="uris" array={uris} />
      <DetailDescriptionLink
        name="scopes"
        array={scope}
        convert={(s) => s.name}
        link={(scope) =>
          toScopeDetails({ id: clientId, realm, scopeId: scope.id! })
        }
      />
      <DetailDescriptionLink
        name="associatedPermissions"
        array={permissions}
        convert={(p) => p.name!}
        link={(permission) =>
          toPermissionDetails({
            id: clientId,
            realm,
            permissionId: permission.id!,
            permissionType: "resource",
          })
        }
      />
    </DescriptionList>
  );
};
