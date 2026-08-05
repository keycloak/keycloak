import type UserRepresentation from "@keycloak/keycloak-admin-client/lib/defs/userRepresentation";
import {
  KeycloakDataTable,
  ListEmptyState,
  useAlerts,
} from "@keycloak/keycloak-ui-shared";
import {
  AlertVariant,
  Button,
  ButtonVariant,
  PageSection,
  ToolbarItem,
} from "@patternfly/react-core";
import { useState } from "react";
import { useTranslation } from "react-i18next";
import { Link } from "react-router-dom";
import { useAdminClient } from "../admin-client";
import { useConfirmDialog } from "../components/confirm-dialog/ConfirmDialog";
import { useRealm } from "../context/realm-context/RealmContext";
import { MemberModal } from "../groups/MembersModal";
import { toUser } from "../user/routes/User";
import { emptyFormatter } from "../util";

type OrganizationRoleUsersProps = {
  organizationId: string;
  roleId: string;
  canMapRole: boolean;
};

export const OrganizationRoleUsers = ({
  organizationId,
  roleId,
  canMapRole,
}: OrganizationRoleUsersProps) => {
  const { t } = useTranslation();
  const { adminClient } = useAdminClient();
  const { addAlert, addError } = useAlerts();
  const { realm } = useRealm();
  const [key, setKey] = useState(0);
  const [showAdd, setShowAdd] = useState(false);
  const [selected, setSelected] = useState<UserRepresentation[]>([]);
  const refresh = () => setKey((value) => value + 1);
  const canManageUser = (user: UserRepresentation) =>
    user.access?.manage ?? canMapRole;

  const loader = async (first?: number, max?: number) => {
    try {
      return await adminClient.organizations.listRoleUsers({
        orgId: organizationId,
        roleId,
        first,
        max,
        briefRepresentation: true,
      });
    } catch (error) {
      addError("organizationRoleUsersLoadError", error);
      return [];
    }
  };

  const availableUsersLoader = async (
    first?: number,
    max?: number,
    search?: string,
  ) => {
    try {
      return await adminClient.organizations.listAvailableRoleUsers({
        orgId: organizationId,
        roleId,
        first,
        max,
        search,
        briefRepresentation: true,
      });
    } catch (error) {
      addError("organizationRoleUsersLoadError", error);
      return [];
    }
  };

  const addUsers = async (users: UserRepresentation[]) => {
    try {
      await adminClient.organizations.addRoleUsers(
        { orgId: organizationId, roleId },
        users.map(({ id }) => ({ id })),
      );
      addAlert(
        t("organizationRoleUsersAdded", { count: users.length }),
        AlertVariant.success,
      );
      refresh();
    } catch (error) {
      addError("organizationRoleUsersAddError", error);
    }
  };

  const [toggleRemoveDialog, RemoveConfirm] = useConfirmDialog({
    titleKey: "removeOrganizationRoleUsersTitle",
    messageKey: t("removeOrganizationRoleUsersConfirm", {
      count: selected.length,
    }),
    continueButtonLabel: "remove",
    continueButtonVariant: ButtonVariant.danger,
    onCancel: () => setSelected([]),
    onConfirm: async () => {
      try {
        await adminClient.organizations.delRoleUsers(
          { orgId: organizationId, roleId },
          selected.map(({ id }) => ({ id })),
        );
        addAlert(
          t("organizationRoleUsersRemoved", { count: selected.length }),
          AlertVariant.success,
        );
        setSelected([]);
        refresh();
      } catch (error) {
        addError("organizationRoleUsersRemoveError", error);
      }
    },
  });

  return (
    <PageSection variant="light">
      <RemoveConfirm />
      {showAdd && (
        <MemberModal
          orgId={organizationId}
          membersQuery={loader}
          availableUsersQuery={availableUsersLoader}
          titleKey="addOrganizationRoleUsers"
          confirmLabelKey="assign"
          canSelectUser={canManageUser}
          onAdd={addUsers}
          onClose={() => setShowAdd(false)}
        />
      )}
      <KeycloakDataTable
        key={key}
        loader={loader}
        isPaginated
        canSelectAll={canMapRole}
        onSelect={
          canMapRole
            ? (users) => setSelected(users.filter(canManageUser))
            : undefined
        }
        isRowDisabled={(user) => !canManageUser(user)}
        ariaLabelKey="organizationRoleUsers"
        toolbarItem={
          canMapRole && (
            <>
              <ToolbarItem>
                <Button onClick={() => setShowAdd(true)}>
                  {t("addOrganizationRoleUsers")}
                </Button>
              </ToolbarItem>
              <ToolbarItem>
                <Button
                  variant="link"
                  isDisabled={selected.length === 0}
                  onClick={toggleRemoveDialog}
                >
                  {t("remove")}
                </Button>
              </ToolbarItem>
            </>
          )
        }
        actions={
          canMapRole
            ? [
                {
                  title: t("remove"),
                  onRowClick: (user) => {
                    setSelected([user]);
                    toggleRemoveDialog();
                  },
                },
              ]
            : undefined
        }
        columns={[
          {
            name: "username",
            displayKey: "username",
            cellRenderer: (user) => (
              <Link to={toUser({ realm, id: user.id!, tab: "settings" })}>
                {user.username}
              </Link>
            ),
          },
          {
            name: "email",
            displayKey: "email",
            cellFormatters: [emptyFormatter()],
          },
          {
            name: "lastName",
            displayKey: "lastName",
            cellFormatters: [emptyFormatter()],
          },
          {
            name: "firstName",
            displayKey: "firstName",
            cellFormatters: [emptyFormatter()],
          },
        ]}
        emptyState={
          <ListEmptyState
            message={t("noOrganizationRoleUsers")}
            instructions={
              canMapRole ? t("noOrganizationRoleUsersInstructions") : ""
            }
            primaryActionText={canMapRole ? t("addOrganizationRoleUsers") : ""}
            onPrimaryAction={canMapRole ? () => setShowAdd(true) : undefined}
          />
        }
      />
    </PageSection>
  );
};
