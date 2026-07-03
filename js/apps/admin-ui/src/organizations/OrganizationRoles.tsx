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
  Label,
  PageSection,
  ToolbarItem,
} from "@patternfly/react-core";
import type { IRowData } from "@patternfly/react-table";
import { useState } from "react";
import { useTranslation } from "react-i18next";
import { Link } from "react-router-dom";
import { useAdminClient } from "../admin-client";
import { useConfirmDialog } from "../components/confirm-dialog/ConfirmDialog";
import { useAccess } from "../context/access/Access";
import { useRealm } from "../context/realm-context/RealmContext";
import { emptyFormatter, upperCaseFormatter } from "../util";
import { translationFormatter } from "../utils/translationFormatter";
import { useParams } from "../utils/useParams";
import { CreateOrganizationRoleModal } from "./CreateOrganizationRoleModal";
import type { EditOrganizationParams } from "./routes/EditOrganization";
import { toOrganizationRole } from "./routes/OrganizationRole";

type OrganizationRoleRow = RoleRepresentation & { isDefault: boolean };

type OrganizationRolesProps = {
  canCreateRole?: boolean;
};

export const OrganizationRoles = ({
  canCreateRole,
}: OrganizationRolesProps) => {
  const { t } = useTranslation();
  const { adminClient } = useAdminClient();
  const { addAlert, addError } = useAlerts();
  const { hasAccess } = useAccess();
  const { realm } = useRealm();
  const { id: orgId } = useParams<EditOrganizationParams>();
  const legacyCanManageOrganizations = hasAccess("manage-organizations");
  const canCreate = canCreateRole ?? legacyCanManageOrganizations;
  const canManageRole = (role: OrganizationRoleRow) =>
    role.access?.manage ?? legacyCanManageOrganizations;

  const [key, setKey] = useState(0);
  const [createOpen, setCreateOpen] = useState(false);
  const [selectedRole, setSelectedRole] = useState<OrganizationRoleRow>();
  const refresh = () => setKey((value) => value + 1);

  const loader = async (first?: number, max?: number, search?: string) => {
    try {
      const [roles, defaultRole] = await Promise.all([
        adminClient.organizations.listRoles({
          orgId,
          first,
          max,
          search,
        }),
        adminClient.organizations.findDefaultRole({ orgId }),
      ]);
      return roles.map((role) => ({
        ...role,
        isDefault: role.id === defaultRole?.id,
      }));
    } catch (error) {
      addError("organizationRolesLoadError", error);
      return [];
    }
  };

  const [toggleDeleteDialog, DeleteConfirm] = useConfirmDialog({
    titleKey: "roleDeleteConfirm",
    messageKey: t("roleDeleteConfirmDialog", {
      selectedRoleName: selectedRole?.name ?? "",
    }),
    continueButtonLabel: "delete",
    continueButtonVariant: ButtonVariant.danger,
    onConfirm: async () => {
      try {
        await adminClient.organizations.delRole({
          orgId,
          roleId: selectedRole!.id!,
        });
        addAlert(t("roleDeletedSuccess"), AlertVariant.success);
        setSelectedRole(undefined);
        refresh();
      } catch (error) {
        addError("roleDeleteError", error);
      }
    },
  });

  return (
    <PageSection variant="light" padding={{ default: "noPadding" }}>
      <DeleteConfirm />
      {createOpen && (
        <CreateOrganizationRoleModal
          organizationId={orgId}
          onClose={() => setCreateOpen(false)}
          onCreated={() => {
            setCreateOpen(false);
            refresh();
          }}
        />
      )}
      <KeycloakDataTable
        key={key}
        loader={loader}
        isPaginated
        ariaLabelKey="organizationRoles"
        searchPlaceholderKey="searchForRoles"
        toolbarItem={
          canCreate && (
            <ToolbarItem>
              <Button
                data-testid="create-organization-role"
                onClick={() => setCreateOpen(true)}
              >
                {t("createRole")}
              </Button>
            </ToolbarItem>
          )
        }
        actionResolver={(rowData: IRowData) => {
          const role = rowData.data as OrganizationRoleRow;
          return role.isDefault || !canManageRole(role)
            ? []
            : [
                {
                  title: t("delete"),
                  onClick: () => {
                    setSelectedRole(role);
                    toggleDeleteDialog();
                  },
                },
              ];
        }}
        columns={[
          {
            name: "name",
            displayKey: "roleName",
            cellRenderer: (role) => (
              <>
                <Link
                  to={toOrganizationRole({
                    realm,
                    orgId,
                    roleId: role.isDefault ? "default" : role.id!,
                    tab: "details",
                  })}
                >
                  {role.name}
                </Link>{" "}
                {role.isDefault && <Label color="blue">{t("default")}</Label>}
              </>
            ),
          },
          {
            name: "composite",
            displayKey: "composite",
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
            message={t("noOrganizationRoles")}
            instructions={canCreate ? t("noOrganizationRolesInstructions") : ""}
            primaryActionText={canCreate ? t("createRole") : ""}
            onPrimaryAction={canCreate ? () => setCreateOpen(true) : undefined}
          />
        }
      />
    </PageSection>
  );
};
