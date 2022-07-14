import React, { useEffect, useState } from "react";
import { useTranslation } from "react-i18next";
import {
  AlertVariant,
  Badge,
  Button,
  ButtonVariant,
  Checkbox,
  ToolbarItem,
} from "@patternfly/react-core";
import { cellWidth } from "@patternfly/react-table";

import type ClientRepresentation from "@keycloak/keycloak-admin-client/lib/defs/clientRepresentation";
import type RoleRepresentation from "@keycloak/keycloak-admin-client/lib/defs/roleRepresentation";
import type { ClientScopes } from "@keycloak/keycloak-admin-client/lib/resources/clientScopes";
import type { Groups } from "@keycloak/keycloak-admin-client/lib/resources/groups";
import type { Roles } from "@keycloak/keycloak-admin-client/lib/resources/roles";
import type { Clients } from "@keycloak/keycloak-admin-client/lib/resources/clients";
import type KeycloakAdminClient from "@keycloak/keycloak-admin-client";
import { AddRoleMappingModal } from "./AddRoleMappingModal";
import { KeycloakDataTable } from "../table-toolbar/KeycloakDataTable";
import { emptyFormatter, upperCaseFormatter } from "../../util";
import { useAlerts } from "../alert/Alerts";
import { useConfirmDialog } from "../confirm-dialog/ConfirmDialog";
import { useAdminClient } from "../../context/auth/AdminClient";
import { ListEmptyState } from "../list-empty-state/ListEmptyState";
import useSetTimeout from "../../utils/useSetTimeout";
import useToggle from "../../utils/useToggle";

import "./role-mapping.css";

export type CompositeRole = RoleRepresentation & {
  parent: RoleRepresentation;
  isInherited?: boolean;
};

export type Row = {
  client?: ClientRepresentation;
  role: RoleRepresentation | CompositeRole;
};

export const mapRoles = (
  assignedRoles: Row[],
  effectiveRoles: Row[],
  hide: boolean
) => [
  ...(hide
    ? assignedRoles.map((row) => ({
        ...row,
        role: {
          ...row.role,
          isInherited: false,
        },
      }))
    : effectiveRoles.map((row) => ({
        ...row,
        role: {
          ...row.role,
          isInherited:
            assignedRoles.find((r) => r.role.id === row.role.id) === undefined,
        },
      }))),
];

export const ServiceRole = ({ role, client }: Row) => (
  <>
    {client && (
      <Badge isRead className="keycloak-admin--role-mapping__client-name">
        {client.clientId}
      </Badge>
    )}
    {role.name}
  </>
);

export type ResourcesKey = keyof KeycloakAdminClient;

type RoleMappingProps = {
  name: string;
  id: string;
  type: ResourcesKey;
  isManager?: boolean;
  loader: () => Promise<Row[]>;
  save: (rows: Row[]) => Promise<void>;
  onHideRolesToggle: () => void;
};

type DeleteFunctions =
  | keyof Pick<Groups, "delClientRoleMappings" | "delRealmRoleMappings">
  | keyof Pick<
      ClientScopes,
      "delClientScopeMappings" | "delRealmScopeMappings"
    >;

type ListFunction =
  | keyof Pick<
      Groups,
      "listAvailableClientRoleMappings" | "listAvailableRealmRoleMappings"
    >
  | keyof Pick<
      ClientScopes,
      "listAvailableClientScopeMappings" | "listAvailableRealmScopeMappings"
    >
  | keyof Pick<Roles, "find">
  | keyof Pick<Clients, "listRoles">;

type FunctionMapping = { delete: DeleteFunctions[]; list: ListFunction[] };

type ResourceMapping = {
  resource: ResourcesKey;
  functions: FunctionMapping;
};

const groupFunctions: FunctionMapping = {
  delete: ["delClientRoleMappings", "delRealmRoleMappings"],
  list: ["listAvailableClientRoleMappings", "listAvailableRealmRoleMappings"],
};

const clientFunctions: FunctionMapping = {
  delete: ["delClientScopeMappings", "delRealmScopeMappings"],
  list: ["listAvailableClientScopeMappings", "listAvailableRealmScopeMappings"],
};

export const mapping: ResourceMapping[] = [
  {
    resource: "groups",
    functions: groupFunctions,
  },
  {
    resource: "users",
    functions: groupFunctions,
  },
  {
    resource: "clientScopes",
    functions: clientFunctions,
  },
  {
    resource: "clients",
    functions: clientFunctions,
  },
  {
    resource: "roles",
    functions: {
      delete: [],
      list: ["listRoles", "find"],
    },
  },
];

export const castAdminClient = (
  adminClient: KeycloakAdminClient,
  resource: ResourcesKey
) =>
  adminClient[resource] as unknown as {
    [index in DeleteFunctions | ListFunction]: (
      ...params: any
    ) => Promise<RoleRepresentation[]>;
  };

export const RoleMapping = ({
  name,
  id,
  type,
  isManager = true,
  loader,
  save,
  onHideRolesToggle,
}: RoleMappingProps) => {
  const { t } = useTranslation(type);
  const { adminClient } = useAdminClient();
  const { addAlert, addError } = useAlerts();

  const [key, setKey] = useState(0);
  const refresh = () => setKey(key + 1);

  const [hide, setHide] = useState(false);
  const [showAssign, setShowAssign] = useState(false);
  const [selected, setSelected] = useState<Row[]>([]);
  const [wait, toggleWait] = useToggle();
  const setTimeout = useSetTimeout();

  const assignRoles = async (rows: Row[]) => {
    await save(rows);
    refresh();
  };

  useEffect(() => setTimeout(refresh, 200), [wait]);

  const [toggleDeleteDialog, DeleteConfirm] = useConfirmDialog({
    titleKey: "clients:removeMappingTitle",
    messageKey: t("clients:removeMappingConfirm", { count: selected.length }),
    continueButtonLabel: "common:remove",
    continueButtonVariant: ButtonVariant.danger,
    onConfirm: async () => {
      try {
        const mapType = mapping.find((m) => m.resource === type)!;
        await Promise.all(
          selected.map((row) => {
            const role = { id: row.role.id!, name: row.role.name! };
            castAdminClient(adminClient, mapType.resource)[
              mapType.functions.delete[row.client ? 0 : 1]
            ](
              {
                id,
                clientUniqueId: row.client?.id,
                client: row.client?.id,
                roles: [role],
              },
              [role]
            );
          })
        );
        addAlert(t("clients:clientScopeRemoveSuccess"), AlertVariant.success);
        toggleWait();
      } catch (error) {
        addError("clients:clientScopeRemoveError", error);
      }
    },
  });

  const ManagerToolbarItems = () => {
    if (!isManager) return <span />;

    return (
      <>
        <ToolbarItem>
          <Button data-testid="assignRole" onClick={() => setShowAssign(true)}>
            {t("common:assignRole")}
          </Button>
        </ToolbarItem>
        <ToolbarItem>
          <Button
            variant="link"
            data-testid="unAssignRole"
            onClick={toggleDeleteDialog}
            isDisabled={selected.length === 0}
          >
            {t("common:unAssignRole")}
          </Button>
        </ToolbarItem>
      </>
    );
  };

  return (
    <>
      {showAssign && (
        <AddRoleMappingModal
          id={id}
          type={type}
          name={name}
          onAssign={assignRoles}
          onClose={() => setShowAssign(false)}
        />
      )}
      <DeleteConfirm />
      <KeycloakDataTable
        data-testid="assigned-roles"
        key={key}
        loader={loader}
        canSelectAll
        onSelect={(rows) => setSelected(rows)}
        searchPlaceholderKey="clients:searchByName"
        ariaLabelKey="clients:clientScopeList"
        isRowDisabled={(value) =>
          (value.role as CompositeRole).isInherited || false
        }
        toolbarItem={
          <>
            <ToolbarItem>
              <Checkbox
                label={t("common:hideInheritedRoles")}
                id="hideInheritedRoles"
                isChecked={hide}
                onChange={(check) => {
                  setHide(check);
                  onHideRolesToggle();
                  refresh();
                }}
              />
            </ToolbarItem>
            <ManagerToolbarItems />
          </>
        }
        actions={
          isManager
            ? [
                {
                  title: t("common:unAssignRole"),
                  onRowClick: async (role) => {
                    setSelected([role]);
                    toggleDeleteDialog();
                    return false;
                  },
                },
              ]
            : []
        }
        columns={[
          {
            name: "role.name",
            displayKey: t("common:name"),
            transforms: [cellWidth(30)],
            cellRenderer: ServiceRole,
          },
          {
            name: "role.isInherited",
            displayKey: t("common:inherent"),
            cellFormatters: [upperCaseFormatter(), emptyFormatter()],
          },
          {
            name: "role.description",
            displayKey: t("common:description"),
            cellFormatters: [emptyFormatter()],
          },
        ]}
        emptyState={
          <ListEmptyState
            message={t("noRoles")}
            instructions={t("noRolesInstructions")}
            primaryActionText={t("common:assignRole")}
            onPrimaryAction={() => setShowAssign(true)}
          />
        }
      />
    </>
  );
};
