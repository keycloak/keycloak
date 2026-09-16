import type KeycloakAdminClient from "@keycloak/keycloak-admin-client";
import type ClientRepresentation from "@keycloak/keycloak-admin-client/lib/defs/clientRepresentation";
import type RoleRepresentation from "@keycloak/keycloak-admin-client/lib/defs/roleRepresentation";
import type { RoleMappingPayload } from "@keycloak/keycloak-admin-client/lib/defs/roleRepresentation";
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
import { useEffect, useState } from "react";
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
import { deleteMapping, getMapping } from "./queries";
import { getAllEffectiveRoles } from "./resource";

import "./role-mapping.css";

export type CompositeRole = RoleRepresentation & {
  parent: RoleRepresentation;
  isInherited?: boolean;
};

export type Row = {
  client?: ClientRepresentation;
  org?: { id?: string; orgAlias?: string };
  role: RoleRepresentation | CompositeRole;
  id?: string; // KeycloakDataTable expects an id for the row
};

export const isRoleMappingRowDisabled = (
  value: Row,
  canMapOrganizationRoles = false,
) =>
  Boolean((value.role as CompositeRole).isInherited) ||
  Boolean(value.org && !canMapOrganizationRoles);

export const mapRoles = (
  assignedRoles: Row[],
  effectiveRoles: Row[],
  hide: boolean,
) => {
  if (hide) {
    return assignedRoles.map((row) => ({
      id: row.role.id,
      ...row,
      role: {
        ...row.role,
        isInherited: false,
      },
    }));
  }

  const assignedRoleIds = new Set(
    assignedRoles.filter((r) => r.role.id).map((r) => r.role.id),
  );
  const effectiveRoleCountMap = new Map<string, number>();
  effectiveRoles.forEach((row) => {
    if (row.role.id) {
      effectiveRoleCountMap.set(
        row.role.id,
        (effectiveRoleCountMap.get(row.role.id) || 0) + 1,
      );
    }
  });

  const result: Row[] = [];
  const seenRoleIds = new Set<string>();

  effectiveRoles.forEach((row) => {
    if (row.role.id && !seenRoleIds.has(row.role.id)) {
      seenRoleIds.add(row.role.id);
      const isAssigned = assignedRoleIds.has(row.role.id);
      const effectiveCount = effectiveRoleCountMap.get(row.role.id) || 0;

      if (isAssigned && effectiveCount > 1) {
        result.push({
          id: `${row.role.id}-direct`,
          ...row,
          role: {
            ...row.role,
            isInherited: false,
          },
        });
        result.push({
          id: `${row.role.id}-inherited`,
          ...row,
          role: {
            ...row.role,
            isInherited: true,
          },
        });
      } else {
        result.push({
          id: row.role.id,
          ...row,
          role: {
            ...row.role,
            isInherited: !isAssigned,
          },
        });
      }
    }
  });

  return result;
};

export const ServiceRole = ({ role, client, org }: Row) => (
  <>
    {client?.clientId && (
      <Badge isRead className="keycloak-admin--role-mapping__client-name">
        {client.clientId}
      </Badge>
    )}
    {org?.orgAlias && (
      <Badge isRead className="keycloak-admin--role-mapping__client-name">
        {org.orgAlias}
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
  groupsResource?: Groups;
  canMapOrganizationRoles?: boolean;
};

export const RoleMapping = ({
  name,
  id,
  type,
  isManager = true,
  save,
  groupsResource,
  canMapOrganizationRoles = false,
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
  const organizationId = groupsResource?.getOrgId();

  useEffect(() => {
    setSelected([]);
    setShowAssign(false);
  }, [id, type, organizationId]);

  const assignRoles = async (rows: Row[]) => {
    await save(rows);
    refresh();
  };

  const loader = async () => {
    let allEffectiveRoles: Row[] = [];

    if (!hide) {
      const effectiveRoles = await getAllEffectiveRoles(adminClient, {
        type,
        id,
      });

      allEffectiveRoles = effectiveRoles.map((e) => ({
        ...(e.clientRole && e.client && e.clientId
          ? { client: { clientId: e.client, id: e.clientId } }
          : {}),
        role: { id: e.id, name: e.name, description: e.description },
      }));
    }

    const roles = await getMapping(adminClient, type, id, groupsResource);
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
    const orgMapping = Object.entries(roles.organizationMappings || {})
      .map(([orgAlias, orgRoles]) =>
        (orgRoles as RoleRepresentation[]).map((role: RoleRepresentation) => ({
          org: { orgAlias, id: orgAlias },
          role,
        })),
      )
      .flat();

    return [
      ...mapRoles(
        [...clientMapping, ...realmRolesMapping, ...orgMapping],
        allEffectiveRoles,
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
        if (
          selected.some((row) =>
            isRoleMappingRowDisabled(row, canMapOrganizationRoles),
          )
        ) {
          throw new Error(t("roleMappingUpdatedError"));
        }
        const cleanRole = (
          role: RoleRepresentation | CompositeRole,
        ): RoleMappingPayload => {
          if (!role.id || !role.name) {
            throw new Error("Role mappings require both a role id and name");
          }
          return { id: role.id, name: role.name };
        };
        const realmRoles = selected.filter(
          (row) => row.client === undefined && !row.org,
        );
        const clientRoles = selected.filter((row) => row.client !== undefined);
        const orgRoles = selected.filter((row) => row.org);

        if (orgRoles.length > 0 && !groupsResource) {
          throw new Error(
            "Organization role mappings require an organization group",
          );
        }

        await Promise.all([
          ...(realmRoles.length > 0
            ? groupsResource
              ? [
                  groupsResource.delRealmRoleMappings({
                    id,
                    roles: realmRoles.map((row) => cleanRole(row.role)),
                  }),
                ]
              : deleteMapping(adminClient, type, id, realmRoles)
            : []),
          ...(clientRoles.length > 0
            ? groupsResource
              ? clientRoles.map((row) =>
                  groupsResource.delClientRoleMappings({
                    id,
                    clientUniqueId: row.client!.id!,
                    roles: [cleanRole(row.role)],
                  }),
                )
              : deleteMapping(adminClient, type, id, clientRoles)
            : []),
          ...(orgRoles.length > 0
            ? [
                groupsResource!.delOrganizationRoleMappings({
                  id,
                  roles: orgRoles.map((row) => cleanRole(row.role)),
                }),
              ]
            : []),
        ]);
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
          canMapOrganizationRoles={canMapOrganizationRoles}
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
        isRowDisabled={(row) =>
          isRoleMappingRowDisabled(row, canMapOrganizationRoles)
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
                    canMapOrganizationRoles={canMapOrganizationRoles}
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
                    if (
                      isRoleMappingRowDisabled(role, canMapOrganizationRoles)
                    ) {
                      return false;
                    }
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
            {isManager && (
              <AddRoleButton
                canMapOrganizationRoles={canMapOrganizationRoles}
                onFilerTypeChange={(type) => {
                  setFilterType(type);
                  setShowAssign(true);
                }}
              />
            )}
          </ListEmptyState>
        }
      />
    </>
  );
};
