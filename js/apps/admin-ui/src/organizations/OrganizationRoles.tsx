import { Button, PageSection, ToolbarItem } from "@patternfly/react-core";
import { useState } from "react";
import { useTranslation } from "react-i18next";
import { Link } from "react-router-dom";
import { useAdminClient } from "../admin-client";
import { useAlerts, KeycloakDataTable, ListEmptyState } from "@keycloak/keycloak-ui-shared";
import { useConfirmDialog } from "../components/confirm-dialog/ConfirmDialog";
import { useRealm } from "../context/realm-context/RealmContext";
import { useParams } from "../utils/useParams";
import { EditOrganizationParams } from "./routes/EditOrganization";
import { toOrganizationRole } from "./routes/OrganizationRole";
import { CreateOrganizationRoleModal } from "./CreateOrganizationRoleModal";

interface OrganizationRole {
  id?: string;
  name?: string;
  description?: string;
  composite?: boolean;
}

export default function OrganizationRoles() {
  const { t } = useTranslation();
  const { adminClient } = useAdminClient();
  const { addAlert, addError } = useAlerts();
  const { realm } = useRealm();
  const { id } = useParams<EditOrganizationParams>();

  const [selectedRole, setSelectedRole] = useState<OrganizationRole>();
  const [createOpen, setCreateOpen] = useState(false);
  const [key, setKey] = useState(0);

  const refresh = () => setKey(key + 1);

  const loader = async (first?: number, max?: number, search?: string) => {
    try {
      return await adminClient.organizations.listRoles({
        orgId: id,
        first: first || 0,
        max: max || 10,
        search,
      });
    } catch (error) {
      console.error("Failed to load organization roles:", error);
      return [];
    }
  };

  const [toggleDeleteDialog, DeleteConfirm] = useConfirmDialog({
    titleKey: "deleteRole",
    messageKey: "deleteRoleConfirm",
    continueButtonLabel: "delete",
    onConfirm: async () => {
      try {
        await adminClient.organizations.delRole({
          orgId: id,
          roleId: selectedRole!.id!,
        });

        addAlert(t("roleDeletedSuccess"));
        refresh();
      } catch (error) {
        addError("roleDeleteError", error);
      }
    },
  });

  const deleteRole = (role: OrganizationRole) => {
    setSelectedRole(role);
    toggleDeleteDialog();
  };

  return (
    <>
      <DeleteConfirm />
      {createOpen && (
        <CreateOrganizationRoleModal
          organizationId={id}
          onClose={() => setCreateOpen(false)}
          onSuccess={() => {
            setCreateOpen(false);
            refresh();
          }}
        />
      )}
      <PageSection variant="light" padding={{ default: "noPadding" }}>
        <KeycloakDataTable
          key={key}
          loader={loader}
          ariaLabelKey="organizationRoles"
          searchPlaceholderKey="searchForRole"
          toolbarItem={
            <ToolbarItem>
              <Button
                data-testid="create-role"
                onClick={() => setCreateOpen(true)}
              >
                {t("createRole")}
              </Button>
            </ToolbarItem>
          }
          columns={[
            {
              name: "name",
              displayKey: "name",
              cellRenderer: (role: OrganizationRole) => (
                <Link
                  to={toOrganizationRole({
                    realm,
                    orgId: id!,
                    roleId: role.id!,
                    tab: "details",
                  })}
                >
                  {role.name}
                </Link>
              ),
            },
            {
              name: "description",
              displayKey: "description",
              cellRenderer: (role: OrganizationRole) => role.description || "",
            },
            {
              name: "composite",
              displayKey: "composite",
              cellRenderer: (role: OrganizationRole) =>
                role.composite ? t("yes") : t("no"),
            },
          ]}
          actions={[
            {
              title: t("delete"),
              onRowClick: deleteRole,
            },
          ]}
          emptyState={
            <ListEmptyState
              hasIcon={true}
              message={t("noOrganizationRoles")}
              instructions={t("noOrganizationRolesInstructions")}
              primaryActionText={t("createRole")}
              onPrimaryAction={() => setCreateOpen(true)}
            />
          }
        />
      </PageSection>
    </>
  );
}
