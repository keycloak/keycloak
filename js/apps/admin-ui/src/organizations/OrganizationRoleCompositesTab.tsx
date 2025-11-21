import type RoleRepresentation from "@keycloak/keycloak-admin-client/lib/defs/roleRepresentation";
import type { RoleMappingPayload } from "@keycloak/keycloak-admin-client/lib/defs/roleRepresentation";
import {
  KeycloakDataTable,
  ListEmptyState,
  useAlerts,
} from "@keycloak/keycloak-ui-shared";
import {
  AlertVariant,
  Button,
  ButtonVariant,
  Checkbox,
  ToolbarItem,
} from "@patternfly/react-core";
import { cellWidth, IAction } from "@patternfly/react-table";
import { useState } from "react";
import { useTranslation } from "react-i18next";
import { useAdminClient } from "../admin-client";
import { useConfirmDialog } from "../components/confirm-dialog/ConfirmDialog";
import {
  AddRoleButton,
  AddRoleMappingModal,
  FilterType,
} from "../components/role-mapping/AddRoleMappingModal";
import { Row, ServiceRole } from "../components/role-mapping/RoleMapping";
import { useAccess } from "../context/access/Access";
import { emptyFormatter, upperCaseFormatter } from "../util";
import { translationFormatter } from "../utils/translationFormatter";
import { useParams } from "../utils/useParams";
import type { OrganizationRoleParams } from "./routes/OrganizationRole";

export const OrganizationRoleCompositesTab = () => {
  const { adminClient } = useAdminClient();
  const { t } = useTranslation();
  const { addAlert, addError } = useAlerts();
  const { hasAccess } = useAccess();
  const { orgId, roleId } = useParams<OrganizationRoleParams>();

  const [roleName, setRoleName] = useState<string>("");
  const [key, setKey] = useState(0);
  const [hide, setHide] = useState(true);
  const [showAssign, setShowAssign] = useState(false);
  const [filterType, setFilterType] = useState<FilterType>("roles");
  const [selected, setSelected] = useState<Row[]>([]);

  const isManager = hasAccess("manage-users");

  const refresh = () => setKey(key + 1);

  const addComposites = async (composites: RoleRepresentation[]) => {
    if (!orgId || !roleId) return;

    try {
      await adminClient.organizations.addRoleComposites(
        { orgId, roleId },
        composites,
      );
      refresh();
      addAlert(t("organizationRoleCompositeAdded"), AlertVariant.success);
    } catch (error) {
      addError("organizationRoleCompositeAddError", error);
    }
  };

  const assignRoles = async (rows: Row[]) => {
    await addComposites(rows.map((r) => r.role));
    setShowAssign(false);
  };

  const loader = async () => {
    if (!orgId || !roleId) return [];

    try {
      // Load role name if not already loaded
      if (!roleName) {
        const role = await adminClient.organizations.findRole({
          orgId,
          roleId,
        });
        setRoleName(role.name || "");
      }

      // Load composite roles
      const composites = await adminClient.organizations.listRoleComposites({
        orgId,
        roleId,
      });

      return composites.map((role: RoleRepresentation) => ({
        role,
        id: role.id,
      })) as Row[];
    } catch (error) {
      console.error("Error loading organization role composites:", error);
      return [];
    }
  };

  const [toggleDeleteDialog, DeleteConfirm] = useConfirmDialog({
    titleKey: "removeMappingTitle",
    messageKey: t("removeMappingConfirm", { count: selected.length }),
    continueButtonLabel: "remove",
    continueButtonVariant: ButtonVariant.danger,
    onCancel: () => {
      setSelected([]);
    },
    onConfirm: async () => {
      if (!orgId || !roleId) return;

      try {
        await adminClient.organizations.delRoleComposites(
          { orgId, roleId },
          selected.map((s) => s.role as RoleMappingPayload),
        );
        addAlert(t("roleMappingUpdatedSuccess"), AlertVariant.success);
        setSelected([]);
        refresh();
      } catch (error) {
        addError("roleMappingUpdatedError", error);
      }
    },
  });

  if (!orgId || !roleId) {
    return null;
  }

  return (
    <>
      {showAssign && (
        <AddRoleMappingModal
          id={roleId}
          type="roles"
          filterType={filterType}
          name={roleName}
          onAssign={assignRoles}
          onClose={() => setShowAssign(false)}
        />
      )}
      <DeleteConfirm />
      <KeycloakDataTable
        data-testid="organization-role-composites"
        key={`${orgId}-${roleId}-${key}`}
        loader={loader}
        canSelectAll
        onSelect={(rows) => setSelected(rows)}
        searchPlaceholderKey="searchByName"
        ariaLabelKey="roleList"
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
                  onRowClick: async (role: Row) => {
                    setSelected([role]);
                    toggleDeleteDialog();
                    return false;
                  },
                } as IAction,
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
            name: "role.composite",
            displayKey: "composite",
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
            message={t("noRoles-organizationRoles")}
            instructions={t("noRolesInstructions-organizationRoles")}
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
