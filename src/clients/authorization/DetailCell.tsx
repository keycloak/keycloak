import React, { useState } from "react";
import { useTranslation } from "react-i18next";
import {
  DescriptionList,
  DescriptionListGroup,
  DescriptionListTerm,
  DescriptionListDescription,
} from "@patternfly/react-core";

import type ResourceServerRepresentation from "@keycloak/keycloak-admin-client/lib/defs/resourceServerRepresentation";
import { KeycloakSpinner } from "../../components/keycloak-spinner/KeycloakSpinner";
import { useAdminClient, useFetch } from "../../context/auth/AdminClient";

import "./detail-cell.css";

type Scope = { id: string; name: string }[];

type DetailCellProps = {
  id: string;
  clientId: string;
  uris?: string[];
};

export const DetailCell = ({ id, clientId, uris }: DetailCellProps) => {
  const { t } = useTranslation("clients");
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
      <DescriptionListGroup>
        <DescriptionListTerm>{t("uris")}</DescriptionListTerm>
        <DescriptionListDescription>
          {uris?.map((uri) => (
            <span key={uri} className="pf-u-pr-sm">
              {uri}
            </span>
          ))}
          {uris?.length === 0 && <i>{t("common:none")}</i>}
        </DescriptionListDescription>
      </DescriptionListGroup>
      <DescriptionListGroup>
        <DescriptionListTerm>{t("scopes")}</DescriptionListTerm>
        <DescriptionListDescription>
          {scope.map((scope) => (
            <span key={scope.id} className="pf-u-pr-sm">
              {scope.name}
            </span>
          ))}
          {scope.length === 0 && <i>{t("common:none")}</i>}
        </DescriptionListDescription>
      </DescriptionListGroup>
      <DescriptionListGroup>
        <DescriptionListTerm>{t("associatedPermissions")}</DescriptionListTerm>
        <DescriptionListDescription>
          {permissions.map((permission) => (
            <span key={permission.id} className="pf-u-pr-sm">
              {permission.name}
            </span>
          ))}
          {permissions.length === 0 && <i>{t("common:none")}</i>}
        </DescriptionListDescription>
      </DescriptionListGroup>
    </DescriptionList>
  );
};
