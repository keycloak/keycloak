import React, { useState } from "react";
import { useTranslation } from "react-i18next";
import {
  AlertVariant,
  Badge,
  Button,
  ButtonVariant,
  Checkbox,
  ToolbarItem,
} from "@patternfly/react-core";

import type ClientRepresentation from "@keycloak/keycloak-admin-client/lib/defs/clientRepresentation";
import type RoleRepresentation from "@keycloak/keycloak-admin-client/lib/defs/roleRepresentation";
import { AddRoleMappingModal, MappingType } from "./AddRoleMappingModal";
import { KeycloakDataTable } from "../table-toolbar/KeycloakDataTable";
import { emptyFormatter } from "../../util";

import "./role-mapping.css";
import { useConfirmDialog } from "../confirm-dialog/ConfirmDialog";
import { useAdminClient } from "../../context/auth/AdminClient";
import { useAlerts } from "../alert/Alerts";
import { ListEmptyState } from "../list-empty-state/ListEmptyState";

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
) => {
  return [
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
              assignedRoles.find((r) => r.role.id === row.role.id) ===
              undefined,
          },
        }))),
  ];
};

export const ServiceRole = ({ role, client }: Row) => (
  <>
    {client && (
      <Badge
        key={`${client.id}-${role.id}`}
        isRead
        className="keycloak-admin--role-mapping__client-name"
      >
        {client.clientId}
      </Badge>
    )}
    {role.name}
  </>
);

type RoleMappingProps = {
  name: string;
  id: string;
  type: MappingType;
  loader: () => Promise<Row[]>;
  save: (rows: Row[]) => Promise<void>;
  onHideRolesToggle: () => void;
};

export const RoleMapping = ({
  name,
  id,
  type,
  loader,
  save,
  onHideRolesToggle,
}: RoleMappingProps) => {
  const { t } = useTranslation("clients");
  const adminClient = useAdminClient();
  const { addAlert, addError } = useAlerts();

  const [key, setKey] = useState(0);
  const refresh = () => setKey(new Date().getTime());

  const [hide, setHide] = useState(false);
  const [showAssign, setShowAssign] = useState(false);
  const [selected, setSelected] = useState<Row[]>([]);

  const assignRoles = async (rows: Row[]) => {
    await save(rows);
    refresh();
  };

  const [toggleDeleteDialog, DeleteConfirm] = useConfirmDialog({
    titleKey: "clients:removeMappingTitle",
    messageKey: t("removeMappingConfirm", { count: selected.length }),
    continueButtonLabel: "common:remove",
    continueButtonVariant: ButtonVariant.danger,
    onConfirm: async () => {
      try {
        switch (type) {
          case "group":
            await Promise.all(
              selected.map((row) => {
                const role = { id: row.role.id!, name: row.role.name! };
                if (row.client) {
                  return adminClient.groups.delClientRoleMappings({
                    id,
                    clientUniqueId: row.client!.id!,
                    roles: [role],
                  });
                } else {
                  return adminClient.groups.delRealmRoleMappings({
                    id,
                    roles: [role],
                  });
                }
              })
            );
            break;
          case "service-account":
            await Promise.all(
              selected.map((row) => {
                const role = { id: row.role.id!, name: row.role.name! };
                if (row.client) {
                  return adminClient.users.delClientRoleMappings({
                    id,
                    clientUniqueId: row.client!.id!,
                    roles: [role],
                  });
                } else {
                  return adminClient.users.delRealmRoleMappings({
                    id,
                    roles: [role],
                  });
                }
              })
            );
            break;
          case "client-scope":
            await Promise.all(
              selected.map((row) => {
                const role = { id: row.role.id!, name: row.role.name! };
                if (row.client) {
                  return adminClient.clientScopes.delClientScopeMappings(
                    {
                      id,
                      client: row.client!.id!,
                    },
                    [role]
                  );
                } else {
                  return adminClient.clientScopes.delRealmScopeMappings(
                    {
                      id,
                    },
                    [role]
                  );
                }
              })
            );
            break;
        }
        addAlert(t("clientScopeRemoveSuccess"), AlertVariant.success);
        refresh();
      } catch (error) {
        addError("clients:clientScopeRemoveError", error);
      }
    },
  });

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
                label={t("hideInheritedRoles")}
                id="hideInheritedRoles"
                isChecked={hide}
                onChange={(check) => {
                  setHide(check);
                  onHideRolesToggle();
                  refresh();
                }}
              />
            </ToolbarItem>
            <ToolbarItem>
              <Button
                data-testid="assignRole"
                onClick={() => setShowAssign(true)}
              >
                {t("assignRole")}
              </Button>
            </ToolbarItem>
            <ToolbarItem>
              <Button
                variant="link"
                data-testid="unAssignRole"
                onClick={toggleDeleteDialog}
                isDisabled={selected.length === 0}
              >
                {t("unAssignRole")}
              </Button>
            </ToolbarItem>
          </>
        }
        actions={[
          {
            title: t("unAssignRole"),
            onRowClick: async (role) => {
              setSelected([role]);
              toggleDeleteDialog();
              return false;
            },
          },
        ]}
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
        emptyState={
          <ListEmptyState
            message={t("noRoles")}
            instructions={t("noRolesInstructions")}
            primaryActionText={t("assignRole")}
            onPrimaryAction={() => setShowAssign(true)}
          />
        }
      />
    </>
  );
};
