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
  Checkbox,
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
import { emptyFormatter, upperCaseFormatter } from "../util";
import { translationFormatter } from "../utils/translationFormatter";

type CompositeSource = "organization" | "realm" | "client";

type CompositeRow = RoleRepresentation & {
  source: CompositeSource;
  clientName?: string;
  isInherited?: boolean;
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
  delete (representation as Partial<CompositeRow>).isInherited;
  return representation;
};

type AddOrganizationRoleCompositeModalProps = {
  organizationId: string;
  roleId: string;
  roleName: string;
  onAssign: (roles: RoleRepresentation[]) => Promise<boolean>;
  onClose: () => void;
};

export const AddOrganizationRoleCompositeModal = ({
  organizationId,
  roleId,
  roleName,
  onAssign,
  onClose,
}: AddOrganizationRoleCompositeModalProps) => {
  const { t } = useTranslation();
  const { adminClient } = useAdminClient();
  const [source, setSource] = useState<CompositeSource>("organization");
  const [selected, setSelected] = useState<CompositeRow[]>([]);
  const [isAssigning, setIsAssigning] = useState(false);

  const loader = async (first?: number, max?: number, search?: string) => {
    const roles = await adminClient.organizations.listAvailableRoleComposites({
      orgId: organizationId,
      roleId,
      source,
      first,
      max,
      search,
    });
    return roles.map((role) => ({ ...role, source }));
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
  canManage: boolean;
};

export const OrganizationRoleComposites = ({
  organizationId,
  roleId,
  roleName,
  canManage,
}: OrganizationRoleCompositesProps) => {
  const { t } = useTranslation();
  const { adminClient } = useAdminClient();
  const { addAlert, addError } = useAlerts();
  const [key, setKey] = useState(0);
  const [hideInherited, setHideInherited] = useState(true);
  const [showAssign, setShowAssign] = useState(false);
  const [selected, setSelected] = useState<CompositeRow[]>([]);
  const refresh = () => setKey((value) => value + 1);

  const loader = async (first?: number, max?: number, search?: string) => {
    try {
      if (hideInherited) {
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
          isInherited: false,
        }));
      }

      const [effectiveRoles, directRoles] = await Promise.all([
        adminClient.organizations.listEffectiveRoleComposites({
          orgId: organizationId,
          roleId,
          first,
          max,
          search,
        }),
        adminClient.organizations.listRoleComposites({
          orgId: organizationId,
          roleId,
        }),
      ]);
      const directRoleIds = new Set(directRoles.map((role) => role.id));
      return effectiveRoles.map((role) => ({
        ...role,
        source: sourceKey(role, organizationId),
        isInherited: !directRoleIds.has(role.id),
      }));
    } catch (error) {
      addError("organizationRoleCompositesLoadError", error);
      return [];
    }
  };

  const openAssign = () => setShowAssign(true);

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
          selected.map(toRoleRepresentation),
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
          onAssign={addComposites}
          onClose={() => setShowAssign(false)}
        />
      )}
      <KeycloakDataTable
        key={key}
        loader={loader}
        isPaginated
        canSelectAll={canManage}
        onSelect={
          canManage
            ? (roles) => setSelected(roles.filter((role) => !role.isInherited))
            : undefined
        }
        isRowDisabled={(role) => role.isInherited || false}
        ariaLabelKey="organizationRoleComposites"
        searchPlaceholderKey="searchForRoles"
        toolbarItem={
          <>
            <ToolbarItem>
              <Checkbox
                label={t("hideInheritedRoles")}
                id="hideInheritedRoles"
                data-testid="hideInheritedRoles"
                isChecked={hideInherited}
                onChange={(_event, checked) => {
                  setHideInherited(checked);
                  setSelected([]);
                  refresh();
                }}
              />
            </ToolbarItem>
            {canManage && (
              <>
                <ToolbarItem>
                  <Button onClick={openAssign}>
                    {t("addAssociatedRoles")}
                  </Button>
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
            )}
          </>
        }
        actions={
          canManage
            ? [
                {
                  title: t("unAssignRole"),
                  onRowClick: (role) => {
                    if (role.isInherited) {
                      return false;
                    }
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
            name: "isInherited",
            displayKey: "inherent",
            cellFormatters: [upperCaseFormatter(), emptyFormatter()],
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
              canManage ? t("noOrganizationRoleCompositesInstructions") : ""
            }
            primaryActionText={canManage ? t("addAssociatedRoles") : ""}
            onPrimaryAction={canManage ? openAssign : undefined}
            secondaryActions={[
              {
                text: t("showInheritedRoles"),
                onClick: () => {
                  setHideInherited(false);
                  setSelected([]);
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
