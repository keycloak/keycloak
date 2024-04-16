import type KeycloakAdminClient from "@keycloak/keycloak-admin-client";
import type ClientRepresentation from "@keycloak/keycloak-admin-client/lib/defs/clientRepresentation";
import type RoleRepresentation from "@keycloak/keycloak-admin-client/lib/defs/roleRepresentation";
import {
  AlertVariant,
  Badge,
  Button,
  ButtonVariant,
  Checkbox,
  ToolbarItem,
} from "@patternfly/react-core";
import { cellWidth } from "@patternfly/react-table";
import { useState } from "react";
import { useTranslation } from "react-i18next";

import { emptyFormatter, upperCaseFormatter } from "../../util";
import { useAlerts } from "../alert/Alerts";
import { useConfirmDialog } from "../confirm-dialog/ConfirmDialog";
import { ListEmptyState } from "../list-empty-state/ListEmptyState";
import { Action, KeycloakDataTable } from "../table-toolbar/KeycloakDataTable";
import { AddRoleMappingModal } from "./AddRoleMappingModal";
import { deleteMapping, getEffectiveRoles, getMapping } from "./queries";
import { getEffectiveClientRoles } from "./resource";

import "./role-mapping.css";

export type CompositeRole = RoleRepresentation & {
  parent: RoleRepresentation;
  isInherited?: boolean;
};

export type Row = {
  client?: ClientRepresentation;
  role: RoleRepresentation | CompositeRole;
  id?: string; // KeycloakDataTable expects an id for the row
};

export const mapRoles = (
  assignedRoles: Row[],
  effectiveRoles: Row[],
  hide: boolean,
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
    {client?.clientId && (
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
  save: (rows: Row[]) => Promise<void>;
};

export const RoleMapping = ({
  name,
  id,
  type,
  isManager = true,
  save,
}: RoleMappingProps) => {
  const { t } = useTranslation();
  const { addAlert, addError } = useAlerts();

  const [key, setKey] = useState(0);
  const refresh = () => setKey(key + 1);

  const [hide, setHide] = useState(true);
  const [showAssign, setShowAssign] = useState(false);
  const [selected, setSelected] = useState<Row[]>([]);

  const assignRoles = async (rows: Row[]) => {
    await save(rows);
    refresh();
  };

  const loader = async () => {
    let effectiveRoles: Row[] = [];
    let effectiveClientRoles: Row[] = [];
    if (!hide) {
      effectiveRoles = await getEffectiveRoles(type, id);

      effectiveClientRoles = (
        await getEffectiveClientRoles({
          type,
          id,
        })
      ).map((e) => ({
        client: { clientId: e.client, id: e.clientId },
        role: { id: e.id, name: e.role, description: e.description },
      }));
    }

    const roles = await getMapping(type, id);
    const realmRolesMapping =
      roles.realmMappings?.map((role) => ({ role })) || [];
    const clientMapping = Object.values(roles.clientMappings || {})
      .map((client) =>
        client.mappings.map((role: RoleRepresentation) => ({
          client: { clientId: client.client, ...client },
          role,
        })),
      )
      .flat();

    return [
      ...mapRoles(
        [...realmRolesMapping, ...clientMapping],
        [...effectiveClientRoles, ...effectiveRoles],
        hide,
      ),
    ];
  };

  const [toggleDeleteDialog, DeleteConfirm] = useConfirmDialog({
    titleKey: "removeMappingTitle",
    messageKey: t("removeMappingConfirm", { count: selected.length }),
    continueButtonLabel: "remove",
    continueButtonVariant: ButtonVariant.danger,
    onCancel: () => {
      setSelected([]);
      refresh();
    },
    onConfirm: async () => {
      try {
        await Promise.all(deleteMapping(type, id, selected));
        addAlert(t("clientScopeRemoveSuccess"), AlertVariant.success);
        refresh();
      } catch (error) {
        addError("clientScopeRemoveError", error);
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
        key={`${id}${key}`}
        loader={loader}
        canSelectAll
        onSelect={(rows) => setSelected(rows)}
        searchPlaceholderKey="searchByName"
        ariaLabelKey="clientScopeList"
        isRowDisabled={(value) =>
          (value.role as CompositeRole).isInherited || false
        }
        toolbarItem={
          <>
            <ToolbarItem>
              <Checkbox
                label={t("hideInheritedRoles")}
                id="hideInheritedRoles"
                data-testid="hideInheritedRoles"
                isChecked={hide}
                onChange={(_event, check) => {
                  setHide(check);
                  refresh();
                }}
              />
            </ToolbarItem>
            {isManager && (
              <>
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
            )}
          </>
        }
        actions={
          isManager
            ? [
                {
                  title: t("unAssignRole"),
                  onRowClick: async (role) => {
                    setSelected([role]);
                    toggleDeleteDialog();
                    return false;
                  },
                } as Action<Awaited<ReturnType<typeof loader>>[0]>,
              ]
            : []
        }
        columns={[
          {
            name: "role.name",
            displayKey: t("name"),
            transforms: [cellWidth(30)],
            cellRenderer: ServiceRole,
          },
          {
            name: "role.isInherited",
            displayKey: t("inherent"),
            cellFormatters: [upperCaseFormatter(), emptyFormatter()],
          },
          {
            name: "role.description",
            displayKey: t("description"),
            cellFormatters: [emptyFormatter()],
          },
        ]}
        emptyState={
          <ListEmptyState
            message={t(`noRoles-${type}`)}
            instructions={t(`noRolesInstructions-${type}`)}
            primaryActionText={t("assignRole")}
            onPrimaryAction={() => setShowAssign(true)}
            secondaryActions={[
              {
                text: t("showInheritedRoles"),
                onClick: () => {
                  setHide(false);
                  refresh();
                },
              },
            ]}
          />
        }
      />
    </>
  );
};
