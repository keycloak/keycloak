import type RoleRepresentation from "@keycloak/keycloak-admin-client/lib/defs/roleRepresentation";
import {
  KeycloakDataTable,
  ListEmptyState,
  useAlerts,
} from "@keycloak/keycloak-ui-shared";
import {
  AlertVariant,
  Button,
  ButtonVariant,
  Modal,
  ModalVariant,
  ToggleGroup,
  ToggleGroupItem,
  ToolbarItem,
} from "@patternfly/react-core";
import { useState } from "react";
import { useTranslation } from "react-i18next";
import { useAdminClient } from "../admin-client";
import { useConfirmDialog } from "../components/confirm-dialog/ConfirmDialog";
import { getAvailableClientRoles } from "../components/role-mapping/resource";
import { emptyFormatter } from "../util";
import { translationFormatter } from "../utils/translationFormatter";

type CompositeSource = "organization" | "realm" | "client";

type CompositeRow = RoleRepresentation & {
  source: CompositeSource;
  clientName?: string;
};

const sourceKey = (
  role: RoleRepresentation,
  organizationId: string,
): CompositeSource => {
  if (role.clientRole) {
    return "client";
  }
  return role.containerId === organizationId ? "organization" : "realm";
};

const toRoleRepresentation = (role: CompositeRow): RoleRepresentation => {
  const representation: RoleRepresentation = { ...role };
  delete (representation as Partial<CompositeRow>).source;
  delete (representation as Partial<CompositeRow>).clientName;
  return representation;
};

type AddOrganizationRoleCompositeModalProps = {
  organizationId: string;
  roleId: string;
  roleName: string;
  assignedRoleIds: Set<string>;
  onAssign: (roles: RoleRepresentation[]) => Promise<boolean>;
  onClose: () => void;
};

export const AddOrganizationRoleCompositeModal = ({
  organizationId,
  roleId,
  roleName,
  assignedRoleIds,
  onAssign,
  onClose,
}: AddOrganizationRoleCompositeModalProps) => {
  const { t } = useTranslation();
  const { adminClient } = useAdminClient();
  const [source, setSource] = useState<CompositeSource>("organization");
  const [selected, setSelected] = useState<CompositeRow[]>([]);
  const [isAssigning, setIsAssigning] = useState(false);

  const loader = async (first?: number, max?: number, search?: string) => {
    let roles: CompositeRow[];
    if (source === "organization") {
      roles = (
        await adminClient.organizations.listRoles({
          orgId: organizationId,
          first,
          max,
          search,
        })
      ).map((role) => ({ ...role, source }));
    } else if (source === "realm") {
      roles = (await adminClient.roles.find({ first, max, search })).map(
        (role) => ({ ...role, source }),
      );
    } else {
      roles = (
        await getAvailableClientRoles(adminClient, {
          id: roleId,
          type: "roles",
          first: first ?? 0,
          max: max ?? 10,
          search,
        })
      ).map((role) => ({
        id: role.id,
        name: role.role,
        description: role.description,
        clientRole: true,
        containerId: role.clientId,
        clientName: role.client,
        source,
      }));
    }

    return roles.filter(
      (role) => role.id !== roleId && !assignedRoleIds.has(role.id!),
    );
  };

  return (
    <Modal
      variant={ModalVariant.large}
      title={t("addAssociatedRolesTo", { role: roleName })}
      isOpen
      onClose={onClose}
      actions={[
        <Button
          data-testid="assign-organization-role-composites"
          key="assign"
          variant="primary"
          isLoading={isAssigning}
          isDisabled={selected.length === 0 || isAssigning}
          onClick={async () => {
            setIsAssigning(true);
            const assigned = await onAssign(selected.map(toRoleRepresentation));
            setIsAssigning(false);
            if (assigned) {
              onClose();
            }
          }}
        >
          {t("assign")}
        </Button>,
        <Button key="cancel" variant="link" onClick={onClose}>
          {t("cancel")}
        </Button>,
      ]}
    >
      <ToggleGroup aria-label={t("roleType")}>
        {(["organization", "realm", "client"] as CompositeSource[]).map(
          (type) => (
            <ToggleGroupItem
              key={type}
              text={t(`${type}Roles`)}
              buttonId={`organization-role-source-${type}`}
              isSelected={source === type}
              onChange={() => {
                setSource(type);
                setSelected([]);
              }}
            />
          ),
        )}
      </ToggleGroup>
      <KeycloakDataTable
        key={source}
        loader={loader}
        isPaginated
        canSelectAll
        onSelect={(roles) => setSelected([...roles])}
        ariaLabelKey="availableOrganizationRoleComposites"
        searchPlaceholderKey="searchForRoles"
        columns={[
          { name: "name", displayKey: "roleName" },
          {
            name: "clientName",
            displayKey: "client",
            cellFormatters: [emptyFormatter()],
          },
          {
            name: "description",
            displayKey: "description",
            cellFormatters: [translationFormatter(t)],
          },
        ]}
        emptyState={
          <ListEmptyState
            message={t("noAvailableOrganizationRoleComposites")}
            instructions={t(
              "noAvailableOrganizationRoleCompositesInstructions",
            )}
          />
        }
      />
    </Modal>
  );
};

type OrganizationRoleCompositesProps = {
  organizationId: string;
  roleId: string;
  roleName: string;
  isManager: boolean;
};

export const OrganizationRoleComposites = ({
  organizationId,
  roleId,
  roleName,
  isManager,
}: OrganizationRoleCompositesProps) => {
  const { t } = useTranslation();
  const { adminClient } = useAdminClient();
  const { addAlert, addError } = useAlerts();
  const [key, setKey] = useState(0);
  const [showAssign, setShowAssign] = useState(false);
  const [assignedRoleIds, setAssignedRoleIds] = useState(new Set<string>());
  const [selected, setSelected] = useState<CompositeRow[]>([]);
  const refresh = () => setKey((value) => value + 1);

  const loader = async (first?: number, max?: number, search?: string) => {
    try {
      const roles = await adminClient.organizations.listRoleComposites({
        orgId: organizationId,
        roleId,
        first,
        max,
        search,
      });
      return roles.map((role) => ({
        ...role,
        source: sourceKey(role, organizationId),
      }));
    } catch (error) {
      addError("organizationRoleCompositesLoadError", error);
      return [];
    }
  };

  const openAssign = async () => {
    try {
      const roles = await adminClient.organizations.listRoleComposites({
        orgId: organizationId,
        roleId,
      });
      setAssignedRoleIds(new Set(roles.map((role) => role.id!)));
      setShowAssign(true);
    } catch (error) {
      addError("organizationRoleCompositesLoadError", error);
    }
  };

  const addComposites = async (roles: RoleRepresentation[]) => {
    try {
      await adminClient.organizations.addRoleComposites(
        { orgId: organizationId, roleId },
        roles,
      );
      addAlert(t("organizationRoleCompositesAdded"), AlertVariant.success);
      refresh();
      return true;
    } catch (error) {
      addError("organizationRoleCompositesAddError", error);
      return false;
    }
  };

  const [toggleRemoveDialog, RemoveConfirm] = useConfirmDialog({
    titleKey: "removeMappingTitle",
    messageKey: t("removeMappingConfirm", { count: selected.length }),
    continueButtonLabel: "remove",
    continueButtonVariant: ButtonVariant.danger,
    onCancel: () => setSelected([]),
    onConfirm: async () => {
      try {
        await adminClient.organizations.delRoleComposites(
          { orgId: organizationId, roleId },
          selected,
        );
        addAlert(t("organizationRoleCompositesRemoved"), AlertVariant.success);
        setSelected([]);
        refresh();
      } catch (error) {
        addError("organizationRoleCompositesRemoveError", error);
      }
    },
  });

  return (
    <>
      <RemoveConfirm />
      {showAssign && (
        <AddOrganizationRoleCompositeModal
          organizationId={organizationId}
          roleId={roleId}
          roleName={roleName}
          assignedRoleIds={assignedRoleIds}
          onAssign={addComposites}
          onClose={() => setShowAssign(false)}
        />
      )}
      <KeycloakDataTable
        key={key}
        loader={loader}
        isPaginated
        canSelectAll={isManager}
        onSelect={(roles) => setSelected([...roles])}
        ariaLabelKey="organizationRoleComposites"
        searchPlaceholderKey="searchForRoles"
        toolbarItem={
          isManager && (
            <>
              <ToolbarItem>
                <Button onClick={openAssign}>{t("addAssociatedRoles")}</Button>
              </ToolbarItem>
              <ToolbarItem>
                <Button
                  variant="link"
                  isDisabled={selected.length === 0}
                  onClick={toggleRemoveDialog}
                >
                  {t("unAssignRole")}
                </Button>
              </ToolbarItem>
            </>
          )
        }
        actions={
          isManager
            ? [
                {
                  title: t("unAssignRole"),
                  onRowClick: (role) => {
                    setSelected([role]);
                    toggleRemoveDialog();
                  },
                },
              ]
            : undefined
        }
        columns={[
          { name: "name", displayKey: "roleName" },
          {
            name: "source",
            displayKey: "roleType",
            cellRenderer: (role) => t(`${role.source}Role`),
          },
          {
            name: "description",
            displayKey: "description",
            cellFormatters: [translationFormatter(t)],
          },
        ]}
        emptyState={
          <ListEmptyState
            message={t("noOrganizationRoleComposites")}
            instructions={
              isManager ? t("noOrganizationRoleCompositesInstructions") : ""
            }
            primaryActionText={isManager ? t("addAssociatedRoles") : ""}
            onPrimaryAction={isManager ? openAssign : undefined}
          />
        }
      />
    </>
  );
};
