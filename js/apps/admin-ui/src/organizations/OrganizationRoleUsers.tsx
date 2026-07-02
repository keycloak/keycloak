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
  isManager: boolean;
};

export const OrganizationRoleUsers = ({
  organizationId,
  roleId,
  isManager,
}: OrganizationRoleUsersProps) => {
  const { t } = useTranslation();
  const { adminClient } = useAdminClient();
  const { addAlert, addError } = useAlerts();
  const { realm } = useRealm();
  const [key, setKey] = useState(0);
  const [showAdd, setShowAdd] = useState(false);
  const [selected, setSelected] = useState<UserRepresentation[]>([]);
  const refresh = () => setKey((value) => value + 1);

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
          titleKey="addOrganizationRoleUsers"
          confirmLabelKey="assign"
          onAdd={addUsers}
          onClose={() => setShowAdd(false)}
        />
      )}
      <KeycloakDataTable
        key={key}
        loader={loader}
        isPaginated
        canSelectAll={isManager}
        onSelect={(users) => setSelected([...users])}
        ariaLabelKey="organizationRoleUsers"
        toolbarItem={
          isManager && (
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
          isManager
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
              isManager ? t("noOrganizationRoleUsersInstructions") : ""
            }
            primaryActionText={isManager ? t("addOrganizationRoleUsers") : ""}
            onPrimaryAction={isManager ? () => setShowAdd(true) : undefined}
          />
        }
      />
    </PageSection>
  );
};
