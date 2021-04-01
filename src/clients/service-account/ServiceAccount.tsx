import React, { useContext, useState } from "react";
import { useTranslation } from "react-i18next";
import {
  AlertVariant,
  Badge,
  Button,
  Checkbox,
  ToolbarItem,
} from "@patternfly/react-core";

import RoleRepresentation, {
  RoleMappingPayload,
} from "keycloak-admin/lib/defs/roleRepresentation";
import ClientRepresentation from "keycloak-admin/lib/defs/clientRepresentation";
import { useAdminClient } from "../../context/auth/AdminClient";
import { RealmContext } from "../../context/realm-context/RealmContext";
import { KeycloakDataTable } from "../../components/table-toolbar/KeycloakDataTable";
import { emptyFormatter } from "../../util";
import { AddServiceAccountModal } from "./AddServiceAccountModal";

import "./service-account.css";
import { useAlerts } from "../../components/alert/Alerts";

type ServiceAccountProps = {
  clientId: string;
};

export type Row = {
  client?: ClientRepresentation;
  role: CompositeRole | RoleRepresentation;
};

export const ServiceRole = ({ role, client }: Row) => (
  <>
    {client && (
      <Badge
        key={`${client.id}-${role.id}`}
        isRead
        className="keycloak-admin--service-account__client-name"
      >
        {client.clientId}
      </Badge>
    )}
    {role.name}
  </>
);

type CompositeRole = RoleRepresentation & {
  parent: RoleRepresentation;
};

export const ServiceAccount = ({ clientId }: ServiceAccountProps) => {
  const { t } = useTranslation("clients");
  const adminClient = useAdminClient();
  const { realm } = useContext(RealmContext);
  const { addAlert } = useAlerts();

  const [key, setKey] = useState(0);
  const refresh = () => setKey(new Date().getTime());

  const [hide, setHide] = useState(false);
  const [serviceAccountId, setServiceAccountId] = useState("");
  const [showAssign, setShowAssign] = useState(false);

  const loader = async () => {
    const serviceAccount = await adminClient.clients.getServiceAccountUser({
      id: clientId,
    });
    setServiceAccountId(serviceAccount.id!);
    const effectiveRoles = await adminClient.users.listCompositeRealmRoleMappings(
      { id: serviceAccount.id! }
    );
    const assignedRoles = await adminClient.users.listRealmRoleMappings({
      id: serviceAccount.id!,
    });

    const clients = await adminClient.clients.find();
    const clientRoles = (
      await Promise.all(
        clients.map(async (client) => {
          return {
            client,
            roles: await adminClient.users.listClientRoleMappings({
              id: serviceAccount.id!,
              clientUniqueId: client.id!,
            }),
          };
        })
      )
    ).filter((rows) => rows.roles.length > 0);

    const findClient = (role: RoleRepresentation) => {
      const row = clientRoles.filter((row) =>
        row.roles.find((r) => r.id === role.id)
      )[0];
      return row ? row.client : undefined;
    };

    const clientRolesFlat = clientRoles.map((row) => row.roles).flat();

    const addInherentData = await (async () =>
      Promise.all(
        effectiveRoles.map(async (role) => {
          const compositeRoles = await adminClient.roles.getCompositeRolesForRealm(
            { realm, id: role.id! }
          );
          return compositeRoles.length > 0
            ? compositeRoles.map((r) => {
                return { ...r, parent: role };
              })
            : { ...role, parent: undefined };
        })
      ))();
    const uniqueRolesWithParent = addInherentData
      .flat()
      .filter(
        (role, index, array) =>
          array.findIndex((r) => r.id === role.id) === index
      );
    return ([
      ...(hide ? assignedRoles : uniqueRolesWithParent),
      ...clientRolesFlat,
    ] as CompositeRole[])
      .sort((r1, r2) => r1.name!.localeCompare(r2.name!))
      .map((role) => {
        return {
          client: findClient(role),
          role,
        } as Row;
      });
  };

  const assignRoles = async (rows: Row[]) => {
    try {
      const realmRoles = rows
        .filter((row) => row.client === undefined)
        .map((row) => row.role as RoleMappingPayload)
        .flat();
      adminClient.users.addRealmRoleMappings({
        id: serviceAccountId,
        roles: realmRoles,
      });
      await Promise.all(
        rows
          .filter((row) => row.client !== undefined)
          .map((row) =>
            adminClient.users.addClientRoleMappings({
              id: serviceAccountId,
              clientUniqueId: row.client!.id!,
              roles: [row.role as RoleMappingPayload],
            })
          )
      );
      addAlert(t("roleMappingUpdatedSuccess"), AlertVariant.success);
      refresh();
    } catch (error) {
      addAlert(
        t("roleMappingUpdatedError", {
          error: error.response?.data?.errorMessage || error,
        }),
        AlertVariant.danger
      );
    }
  };
  return (
    <>
      {showAssign && (
        <AddServiceAccountModal
          clientId={clientId}
          serviceAccountId={serviceAccountId}
          onAssign={assignRoles}
          onClose={() => setShowAssign(false)}
        />
      )}
      <KeycloakDataTable
        data-testid="assigned-roles"
        key={key}
        loader={loader}
        onSelect={() => {}}
        searchPlaceholderKey="clients:searchByName"
        ariaLabelKey="clients:clientScopeList"
        toolbarItem={
          <>
            <ToolbarItem>
              <Checkbox
                label={t("hideInheritedRoles")}
                id="hideInheritedRoles"
                isChecked={hide}
                onChange={setHide}
              />
            </ToolbarItem>
            <ToolbarItem>
              <Button onClick={() => setShowAssign(true)}>
                {t("assignRole")}
              </Button>
            </ToolbarItem>
          </>
        }
        columns={[
          {
            name: "role.name",
            displayKey: t("name"),
            cellRenderer: ServiceRole,
          },
          {
            name: "role.parent.name",
            displayKey: t("inherentFrom"),
            cellFormatters: [emptyFormatter()],
          },
          {
            name: "role.description",
            displayKey: t("description"),
            cellFormatters: [emptyFormatter()],
          },
        ]}
      />
    </>
  );
};
