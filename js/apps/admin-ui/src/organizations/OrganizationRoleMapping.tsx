import type OrganizationRoleRepresentation from "@keycloak/keycloak-admin-client/lib/defs/organizationRoleRepresentation";
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
  Modal,
  ModalVariant,
  ToolbarItem,
} from "@patternfly/react-core";
import { cellWidth } from "@patternfly/react-table";
import { useState } from "react";
import { useTranslation } from "react-i18next";
import { useAdminClient } from "../admin-client";
import { useConfirmDialog } from "../components/confirm-dialog/ConfirmDialog";
import { useAccess } from "../context/access/Access";
import { emptyFormatter, upperCaseFormatter } from "../util";
import { translationFormatter } from "../utils/translationFormatter";

export type OrganizationCompositeRole = OrganizationRoleRepresentation & {
  isInherited?: boolean;
};

export type OrganizationRoleRow = {
  role: OrganizationCompositeRole;
  id: string;
};

interface OrganizationRoleMappingProps {
  orgId: string;
  userId: string;
  name: string;
}

export const OrganizationRoleMapping = ({
  orgId,
  userId,
  name,
}: OrganizationRoleMappingProps) => {
  const { adminClient } = useAdminClient();
  const { t } = useTranslation();
  const { addAlert, addError } = useAlerts();
  const { hasAccess } = useAccess();

  const [hide, setHide] = useState(false);
  const [selected, setSelected] = useState<OrganizationRoleRow[]>([]);
  const [key, setKey] = useState(0);
  const [showAssign, setShowAssign] = useState(false);

  const isManager = hasAccess("manage-users");

  const refresh = () => setKey(key + 1);

  const assignedLoader = async () => {
    try {
      const roles = await (adminClient.organizations as any).getUserRoleMappings({
        orgId,
        userId,
      });
      return roles.map((role: OrganizationRoleRepresentation) => ({
        role: { ...role, isInherited: false },
        id: role.id!,
      })) as OrganizationRoleRow[];
    } catch (error) {
      console.error("Error loading user organization role mappings:", error);
      return [];
    }
  };

  const effectiveLoader = async () => {
    try {
      const roles = await (adminClient.organizations as any).getUserRoleMappingsComposite({
        orgId,
        userId,
      });
      return roles.map((role: OrganizationRoleRepresentation) => ({
        role: { ...role, isInherited: true },
        id: role.id!,
      })) as OrganizationRoleRow[];
    } catch (error) {
      console.error("Error loading effective organization role mappings:", error);
      return [];
    }
  };

  const availableLoader = async () => {
    try {
      const roles = await (adminClient.organizations as any).getUserRoleMappingsAvailable({
        orgId,
        userId,
      });
      return roles.map((role: OrganizationRoleRepresentation) => ({
        role,
        id: role.id!,
      })) as OrganizationRoleRow[];
    } catch (error) {
      console.error("Error loading available organization roles:", error);
      return [];
    }
  };

  const loader = hide ? effectiveLoader : assignedLoader;

  const assignRoles = async (roles: OrganizationRoleRepresentation[]) => {
    try {
      await (adminClient.organizations as any).addUserRoleMappings(
        { orgId, userId },
        roles,
      );
      addAlert(t("roleMappingUpdatedSuccess"), AlertVariant.success);
      refresh();
      setShowAssign(false);
    } catch (error) {
      addError("roleMappingUpdatedError", error);
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
      try {
        await (adminClient.organizations as any).delUserRoleMappings(
          { orgId, userId },
          selected.map((s) => s.role),
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
        <OrganizationRoleAssignModal
          orgId={orgId}
          userId={userId}
          onAssign={assignRoles}
          onClose={() => setShowAssign(false)}
        />
      )}
      <DeleteConfirm />
      <KeycloakDataTable
        data-testid="assigned-organization-roles"
        key={`${userId}${key}`}
        loader={loader}
        canSelectAll
        onSelect={(rows) => setSelected(rows)}
        searchPlaceholderKey="searchByName"
        ariaLabelKey="organizationRoleList"
        isRowDisabled={(value) => value.role.isInherited || false}
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
                    data-testid="assign-organization-role-button"
                    variant={ButtonVariant.primary}
                    onClick={() => setShowAssign(true)}
                  >
                    {t("assignRole")}
                  </Button>
                </ToolbarItem>
                <ToolbarItem>
                  <Button
                    variant="link"
                    data-testid="unassign-organization-role-button"
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
                },
              ]
            : []
        }
        columns={[
          {
            name: "role.name",
            displayKey: "name",
            transforms: [cellWidth(30)],
            cellRenderer: (row) => (
              <span data-testid={`organization-role-${row.role.name}`}>
                {row.role.name}
              </span>
            ),
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
            cellRenderer: (row) => row.role.description || "",
          },
        ]}
        emptyState={
          <ListEmptyState
            message={t("noRoles-organization")}
            instructions={t("noRolesInstructions-organization")}
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
              <Button
                data-testid="assign-organization-role-button"
                variant={ButtonVariant.primary}
                onClick={() => setShowAssign(true)}
              >
                {t("assignRole")}
              </Button>
            )}
          </ListEmptyState>
        }
      />
    </>
  );
};

// Modal component for assigning organization roles
interface OrganizationRoleAssignModalProps {
  orgId: string;
  userId: string;
  onAssign: (roles: OrganizationRoleRepresentation[]) => void;
  onClose: () => void;
}

const OrganizationRoleAssignModal = ({
  orgId,
  userId,
  onAssign,
  onClose,
}: OrganizationRoleAssignModalProps) => {
  const { adminClient } = useAdminClient();
  const { t } = useTranslation();
  const [selectedRoles, setSelectedRoles] = useState<OrganizationRoleRow[]>([]);

  const loader = async () => {
    try {
      const roles = await (adminClient.organizations as any).getUserRoleMappingsAvailable({
        orgId,
        userId,
      });
      return roles.map((role: OrganizationRoleRepresentation) => ({
        role,
        id: role.id!,
      })) as OrganizationRoleRow[];
    } catch (error) {
      console.error("Error loading available organization roles:", error);
      return [];
    }
  };

  const handleAssign = () => {
    onAssign(selectedRoles.map((row) => row.role));
    onClose();
  };

  return (
    <Modal
      variant={ModalVariant.large}
      title={t("assignOrganizationRole")}
      isOpen
      onClose={onClose}
      actions={[
        <Button
          key="assign"
          data-testid="assign-selected-organization-roles"
          variant={ButtonVariant.primary}
          onClick={handleAssign}
          isDisabled={selectedRoles.length === 0}
        >
          {t("assign")}
        </Button>,
        <Button key="cancel" variant={ButtonVariant.link} onClick={onClose}>
          {t("cancel")}
        </Button>,
      ]}
    >
      <KeycloakDataTable
        key="assign-organization-roles"
        loader={loader}
        ariaLabelKey="availableOrganizationRoles"
        searchPlaceholderKey="searchByName"
        canSelectAll
        onSelect={(rows) => setSelectedRoles(rows)}
        columns={[
          {
            name: "name",
            displayKey: "name",
            cellRenderer: (row) => row.role.name || "",
          },
          {
            name: "description",
            displayKey: "description",
            cellRenderer: (row) => row.role.description || "",
          },
        ]}
        emptyState={
          <ListEmptyState
            message={t("noAvailableOrganizationRoles")}
            instructions={t("noAvailableOrganizationRolesInstructions")}
          />
        }
      />
    </Modal>
  );
};
