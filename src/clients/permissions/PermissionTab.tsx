import React, { useEffect, useState } from "react";
import { Link, useHistory } from "react-router-dom";
import { useTranslation } from "react-i18next";
import { Form, FormGroup, PageSection, Switch } from "@patternfly/react-core";
import type { IRowData } from "@patternfly/react-table";

import type { ManagementPermissionReference } from "@keycloak/keycloak-admin-client/lib/defs/managementPermissionReference";
import { KeycloakDataTable } from "../../components/table-toolbar/KeycloakDataTable";
import { useAdminClient } from "../../context/auth/AdminClient";
import { useRealm } from "../../context/realm-context/RealmContext";
import { toPermissionDetails } from "../routes/PermissionDetails";
import { KeycloakSpinner } from "../../components/keycloak-spinner/KeycloakSpinner";
import { HelpItem } from "../../components/help-enabler/HelpItem";

import "./permissions-tab.css";

type PermissionsTabProps = {
  clientId: string;
};

export const PermissionsTab = ({ clientId }: PermissionsTabProps) => {
  const { t } = useTranslation("clients");
  const history = useHistory();
  const adminClient = useAdminClient();
  const { realm } = useRealm();
  const [realmId, setRealmId] = useState("");
  const [permission, setPermission] = useState<ManagementPermissionReference>();

  useEffect(() => {
    Promise.all([
      adminClient.clients.find({
        search: true,
        clientId: realm,
      }),
      adminClient.clients.listFineGrainPermissions({ id: clientId }),
    ]).then(([clients, permission]) => {
      setRealmId(clients[0]?.id!);
      setPermission(permission);
    });
  }, []);

  const PermissionDetailLink = (permission: Record<string, string>) => (
    <Link
      key={permission.id}
      to={toPermissionDetails({
        realm,
        id: realmId,
        permissionType: "scope",
        permissionId: permission.id,
      })}
    >
      {permission.name}
    </Link>
  );

  if (!permission) {
    return <KeycloakSpinner />;
  }

  return (
    <PageSection variant="light" className="pf-u-p-0">
      <PageSection variant="light">
        <Form isHorizontal>
          <FormGroup
            className="permission-label"
            label={t("permissionsEnabled")}
            fieldId="permissionsEnabled"
            labelIcon={
              <HelpItem
                helpText="clients-help:permissionsEnabled"
                fieldLabelId="clients:permissionsEnabled"
              />
            }
          >
            <Switch
              id="permissionsEnabled"
              label={t("common:on")}
              labelOff={t("common:off")}
              isChecked={permission.enabled}
              onChange={async (enabled) => {
                const p = await adminClient.clients.updateFineGrainPermission(
                  { id: clientId },
                  { enabled }
                );
                setPermission(p);
              }}
            />
          </FormGroup>
        </Form>
      </PageSection>
      <KeycloakDataTable
        loader={Object.entries(permission.scopePermissions || {}).map(
          ([name, id]) => ({
            id,
            name,
          })
        )}
        ariaLabelKey="clients:permissions"
        searchPlaceholderKey="clients:searchForPermission"
        actionResolver={(rowData: IRowData) => {
          const permission: Record<string, string> = rowData.data;
          return [
            {
              title: t("common:edit"),
              onClick() {
                history.push(
                  toPermissionDetails({
                    realm,
                    id: realmId,
                    permissionType: "scope",
                    permissionId: permission.id,
                  })
                );
              },
            },
          ];
        }}
        columns={[
          {
            name: "scopeName",
            displayKey: "common:name",
            cellRenderer: PermissionDetailLink,
          },
          {
            name: "description",
            displayKey: "common:description",
            cellRenderer: (permission: Record<string, string>) =>
              t(`scopePermissions.${permission.name}-description`),
          },
        ]}
      />
    </PageSection>
  );
};
