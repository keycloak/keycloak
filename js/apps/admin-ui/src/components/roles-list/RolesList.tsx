import type RealmRepresentation from "@keycloak/keycloak-admin-client/lib/defs/realmRepresentation";
import type RoleRepresentation from "@keycloak/keycloak-admin-client/lib/defs/roleRepresentation";
import { AlertVariant, Button, ButtonVariant } from "@patternfly/react-core";
import { useState } from "react";
import { useTranslation } from "react-i18next";
import { Link, To, useNavigate } from "react-router-dom";

import { useAdminClient, useFetch } from "../../context/auth/AdminClient";
import { useRealm } from "../../context/realm-context/RealmContext";
import { toRealmSettings } from "../../realm-settings/routes/RealmSettings";
import { emptyFormatter, upperCaseFormatter } from "../../util";
import { useAlerts } from "../alert/Alerts";
import { useConfirmDialog } from "../confirm-dialog/ConfirmDialog";
import { HelpItem } from "ui-shared";
import { KeycloakSpinner } from "../keycloak-spinner/KeycloakSpinner";
import { ListEmptyState } from "../list-empty-state/ListEmptyState";
import { KeycloakDataTable } from "../table-toolbar/KeycloakDataTable";

import "./RolesList.css";

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
    search?: string
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
  const { t } = useTranslation(messageBundle);
  const navigate = useNavigate();
  const { adminClient } = useAdminClient();
  const { addAlert, addError } = useAlerts();
  const { realm: realmName } = useRealm();
  const [realm, setRealm] = useState<RealmRepresentation>();

  const [selectedRole, setSelectedRole] = useState<RoleRepresentation>();

  useFetch(
    () => adminClient.realms.findOne({ realm: realmName }),
    (realm) => {
      setRealm(realm);
    },
    []
  );

  const RoleDetailLink = (role: RoleRepresentation) =>
    role.name !== realm?.defaultRole?.name ? (
      <Link to={toDetail(role.id!)}>{role.name}</Link>
    ) : (
      <>
        <Link
          to={toRealmSettings({ realm: realmName, tab: "user-registration" })}
        >
          {role.name}{" "}
        </Link>
        <HelpItem
          helpText={t(`${messageBundle}:defaultRole`)}
          fieldLabelId="defaultRole"
        />
      </>
    );

  const [toggleDeleteDialog, DeleteConfirm] = useConfirmDialog({
    titleKey: "roles:roleDeleteConfirm",
    messageKey: t("roles:roleDeleteConfirmDialog", {
      selectedRoleName: selectedRole ? selectedRole!.name : "",
    }),
    continueButtonLabel: "common:delete",
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
        addError("roles:roleDeleteError", error);
      }
    },
  });

  if (!realm) {
    return <KeycloakSpinner />;
  }

  return (
    <>
      <DeleteConfirm />
      <KeycloakDataTable
        key={selectedRole ? selectedRole.id : "roleList"}
        loader={loader!}
        ariaLabelKey="roles:roleList"
        searchPlaceholderKey="roles:searchFor"
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
            ? []
            : [
                {
                  title: t("common:delete"),
                  onRowClick: (role) => {
                    setSelectedRole(role);
                    if (role.name === realm!.defaultRole!.name) {
                      addAlert(
                        t("defaultRoleDeleteError"),
                        AlertVariant.danger
                      );
                    } else toggleDeleteDialog();
                  },
                },
              ]
        }
        columns={[
          {
            name: "name",
            displayKey: "roles:roleName",
            cellRenderer: RoleDetailLink,
          },
          {
            name: "composite",
            displayKey: "roles:composite",
            cellFormatters: [upperCaseFormatter(), emptyFormatter()],
          },
          {
            name: "description",
            displayKey: "common:description",
            cellFormatters: [emptyFormatter()],
          },
        ]}
        emptyState={
          <ListEmptyState
            hasIcon={true}
            message={t("noRoles")}
            instructions={isReadOnly ? "" : t("noRolesInstructions")}
            primaryActionText={isReadOnly ? "" : t("createRole")}
            onPrimaryAction={() => navigate(toCreate)}
          />
        }
      />
    </>
  );
};
