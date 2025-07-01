import type KeycloakAdminClient from "@keycloak/keycloak-admin-client";
import type ClientRepresentation from "@keycloak/keycloak-admin-client/lib/defs/clientRepresentation";
import type RoleRepresentation from "@keycloak/keycloak-admin-client/lib/defs/roleRepresentation";
import { useAlerts } from "@keycloak/keycloak-ui-shared";
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
import { useAdminClient } from "../../admin-client";
import { emptyFormatter, upperCaseFormatter } from "../../util";
import { translationFormatter } from "../../utils/translationFormatter";
import { useConfirmDialog } from "../confirm-dialog/ConfirmDialog";
import { ListEmptyState } from "@keycloak/keycloak-ui-shared";
import { Action, KeycloakDataTable } from "@keycloak/keycloak-ui-shared";
import {
  AddRoleButton,
  AddRoleMappingModal,
  FilterType,
} from "./AddRoleMappingModal";
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
        id: row.role.id,
        ...row,
        role: {
          ...row.role,
          isInherited: false,
        },
      }))
    : effectiveRoles.map((row) => ({
        id: row.role.id,
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
  const { adminClient } = useAdminClient();

  const { t } = useTranslation();
  const { addAlert, addError } = useAlerts();

  const [key, setKey] = useState(0);
  const refresh = () => setKey(key + 1);

  const [hide, setHide] = useState(true);
  const [showAssign, setShowAssign] = useState(false);
  const [filterType, setFilterType] = useState<FilterType>("clients");
  const [selected, setSelected] = useState<Row[]>([]);

  const assignRoles = async (rows: Row[]) => {
    await save(rows);
    refresh();
  };

  const loader = async () => {
    let effectiveRoles: Row[] = [];
    let effectiveClientRoles: Row[] = [];

    if (!hide) {
      effectiveRoles = await getEffectiveRoles(adminClient, type, id);

      effectiveClientRoles = (
        await getEffectiveClientRoles(adminClient, {
          type,
          id,
        })
      ).map((e) => ({
        client: { clientId: e.client, id: e.clientId },
        role: { id: e.id, name: e.role, description: e.description },
      }));

      effectiveRoles = effectiveRoles.filter(
        (role) =>
          !effectiveClientRoles.some(
            (clientRole) => clientRole.role.id === role.role.id,
          ),
      );
    }

    const roles = await getMapping(adminClient, type, id);
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
        [...clientMapping, ...realmRolesMapping],
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
        await Promise.all(deleteMapping(adminClient, type, id, selected));
        addAlert(t("roleMappingUpdatedSuccess"), AlertVariant.success);
        setSelected([]);
        refresh();
      } catch (error) {
        addError("roleMappingUpdatedError", error);
      }
    },
  });

  return (
    <>
      {showAssign && (
        <AddRoleMappingModal
          id={id}
          type={type}
          filterType={filterType}
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
        ariaLabelKey="roleList"
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
                  <AddRoleButton
                    onFilerTypeChange={(type) => {
                      setFilterType(type);
                      setShowAssign(true);
                    }}
                  />
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
            displayKey: "name",
            transforms: [cellWidth(30)],
            cellRenderer: ServiceRole,
          },
          {
            name: "role.isInherited",
            displayKey: "inherent",
            cellFormatters: [upperCaseFormatter(), emptyFormatter()],
          },
          {
            name: "role.description",
            displayKey: "description",
            cellFormatters: [translationFormatter(t)],
          },
        ]}
        emptyState={
          <ListEmptyState
            message={t(`noRoles-${type}`)}
            instructions={t(`noRolesInstructions-${type}`)}
            secondaryActions={[
              {
                text: t("showInheritedRoles"),
                onClick: () => {
                  setHide(false);
                  refresh();
                },
              },
            ]}
          >
            <AddRoleButton
              onFilerTypeChange={(type) => {
                setFilterType(type);
                setShowAssign(true);
              }}
            />
          </ListEmptyState>
        }
      />
    </>
  );
};
