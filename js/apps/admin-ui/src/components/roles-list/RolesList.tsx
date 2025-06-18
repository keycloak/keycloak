import type RoleRepresentation from "@keycloak/keycloak-admin-client/lib/defs/roleRepresentation";
import { HelpItem, useAlerts } from "@keycloak/keycloak-ui-shared";
import { AlertVariant, Button, ButtonVariant } from "@patternfly/react-core";
import { useState } from "react";
import { useTranslation } from "react-i18next";
import { Link, To, useNavigate } from "react-router-dom";
import { useAdminClient } from "../../admin-client";
import { useAccess } from "../../context/access/Access";
import { useRealm } from "../../context/realm-context/RealmContext";
import { toRealmSettings } from "../../realm-settings/routes/RealmSettings";
import { emptyFormatter, upperCaseFormatter } from "../../util";
import { translationFormatter } from "../../utils/translationFormatter";
import { useConfirmDialog } from "../confirm-dialog/ConfirmDialog";
import { ListEmptyState } from "@keycloak/keycloak-ui-shared";
import { Action, KeycloakDataTable } from "@keycloak/keycloak-ui-shared";

import "./RolesList.css";

type RoleDetailLinkProps = RoleRepresentation & {
  defaultRoleName?: string;
  toDetail: (roleId: string) => To;
  messageBundle?: string;
};

const RoleDetailLink = ({
  defaultRoleName,
  toDetail,
  ...role
}: RoleDetailLinkProps) => {
  const { t } = useTranslation();
  const { realm } = useRealm();
  const { hasAccess, hasSomeAccess } = useAccess();
  const canViewUserRegistration =
    hasAccess("view-realm") && hasSomeAccess("view-clients", "manage-clients");

  return role.name !== defaultRoleName ? (
    <Link to={toDetail(role.id!)}>{role.name}</Link>
  ) : (
    <>
      {canViewUserRegistration ? (
        <Link to={toRealmSettings({ realm, tab: "user-registration" })}>
          {role.name}
        </Link>
      ) : (
        <span>{role.name}</span>
      )}{" "}
      <HelpItem helpText={t("defaultRole")} fieldLabelId="defaultRole" />
    </>
  );
};

type RolesListProps = {
  paginated?: boolean;
  parentRoleId?: string;
  messageBundle?: string;
  isReadOnly: boolean;
  toCreate: To;
  toDetail: (roleId: string) => To;
  loader?: (
    first?: number,
    max?: number,
    search?: string,
  ) => Promise<RoleRepresentation[]>;
};

export const RolesList = ({
  loader,
  paginated = true,
  parentRoleId,
  messageBundle = "roles",
  toCreate,
  toDetail,
  isReadOnly,
}: RolesListProps) => {
  const { adminClient } = useAdminClient();

  const { t } = useTranslation();
  const navigate = useNavigate();
  const { addAlert, addError } = useAlerts();
  const { realmRepresentation: realm } = useRealm();

  const [selectedRole, setSelectedRole] = useState<RoleRepresentation>();

  const [toggleDeleteDialog, DeleteConfirm] = useConfirmDialog({
    titleKey: "roleDeleteConfirm",
    messageKey: t("roleDeleteConfirmDialog", {
      selectedRoleName: selectedRole ? selectedRole!.name : "",
    }),
    continueButtonLabel: "delete",
    continueButtonVariant: ButtonVariant.danger,
    onConfirm: async () => {
      try {
        if (!parentRoleId) {
          await adminClient.roles.delById({
            id: selectedRole!.id!,
          });
        } else {
          await adminClient.roles.delCompositeRoles({ id: parentRoleId }, [
            selectedRole!,
          ]);
        }
        setSelectedRole(undefined);
        addAlert(t("roleDeletedSuccess"), AlertVariant.success);
      } catch (error) {
        addError("roleDeleteError", error);
      }
    },
  });

  return (
    <>
      <DeleteConfirm />
      <KeycloakDataTable
        key={selectedRole ? selectedRole.id : "roleList"}
        loader={loader!}
        ariaLabelKey="roleList"
        searchPlaceholderKey="searchForRoles"
        isPaginated={paginated}
        toolbarItem={
          !isReadOnly && (
            <Button
              data-testid="create-role"
              component={(props) => <Link {...props} to={toCreate} />}
            >
              {t("createRole")}
            </Button>
          )
        }
        actions={
          isReadOnly
            ? undefined
            : [
                {
                  title: t("delete"),
                  onRowClick: (role) => {
                    setSelectedRole(role);
                    if (
                      realm?.defaultRole &&
                      role.name === realm!.defaultRole!.name
                    ) {
                      addAlert(
                        t("defaultRoleDeleteError"),
                        AlertVariant.danger,
                      );
                    } else toggleDeleteDialog();
                  },
                } as Action<RoleRepresentation>,
              ]
        }
        columns={[
          {
            name: "name",
            displayKey: "roleName",
            cellRenderer: (row) => (
              <RoleDetailLink
                {...row}
                defaultRoleName={realm?.defaultRole?.name}
                toDetail={toDetail}
                messageBundle={messageBundle}
              />
            ),
          },
          {
            name: "composite",
            displayKey: "composite",
            cellFormatters: [upperCaseFormatter(), emptyFormatter()],
          },
          {
            name: "description",
            cellFormatters: [translationFormatter(t)],
          },
        ]}
        emptyState={
          <ListEmptyState
            hasIcon={true}
            message={t(`noRoles-${messageBundle}`)}
            instructions={
              isReadOnly ? "" : t(`noRolesInstructions-${messageBundle}`)
            }
            primaryActionText={isReadOnly ? "" : t("createRole")}
            onPrimaryAction={() => navigate(toCreate)}
          />
        }
      />
    </>
  );
};
