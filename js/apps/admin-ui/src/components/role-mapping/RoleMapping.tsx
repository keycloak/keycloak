import type ClientRepresentation from "@keycloak/keycloak-admin-client/lib/defs/clientRepresentation";
import type RoleRepresentation from "@keycloak/keycloak-admin-client/lib/defs/roleRepresentation";
import type { Groups } from "@keycloak/keycloak-admin-client/lib/resources/groups";
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
import useLocaleSort from "../../utils/useLocaleSort";
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
import {
  deleteMapping,
  getInheritedMapping,
  getMapping,
  ResourcesKey,
} from "./queries";

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
  inheritedRoles: Row[],
  hide: boolean,
) => {
  const direct = assignedRoles.map((row) => ({
    id: row.role.id,
    ...row,
    role: {
      ...row.role,
      isInherited: false,
    },
  }));

  if (hide) {
    return direct;
  }

  const assignedRoleIds = new Set(direct.map((row) => row.role.id));

  // a role that is both assigned directly and inherited is listed twice
  const inherited = inheritedRoles.map((row) => ({
    id: assignedRoleIds.has(row.role.id)
      ? `${row.role.id}-inherited`
      : row.role.id,
    ...row,
    role: {
      ...row.role,
      isInherited: true,
    },
  }));

  return [...direct, ...inherited];
};

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

export type { ResourcesKey, RoleMappingType } from "./queries";

type RoleMappingProps = {
  name: string;
  id: string;
  type: ResourcesKey;
  isManager?: boolean;
  save: (rows: Row[]) => Promise<void>;
  groupsResource?: Groups;
};

export const RoleMapping = ({
  name,
  id,
  type,
  isManager = true,
  save,
  groupsResource,
}: RoleMappingProps) => {
  const { adminClient } = useAdminClient();
  const localeSort = useLocaleSort();

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
    const [assignedRoles, inheritedRoles] = await Promise.all([
      getMapping(adminClient, type, id, groupsResource),
      hide ? [] : getInheritedMapping(adminClient, type, id, groupsResource),
    ]);

    return localeSort(
      mapRoles(assignedRoles, inheritedRoles, hide),
      ({ client, role }) => `${client?.clientId ?? ""} ${role.name}`,
    );
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
        await Promise.all(
          deleteMapping(adminClient, type, id, selected, groupsResource),
        );
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
          groupsResource={groupsResource}
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
                    type={type}
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
              type={type}
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
