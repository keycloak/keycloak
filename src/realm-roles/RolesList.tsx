import React, { FunctionComponent, useState } from "react";
import { Link, useHistory, useRouteMatch } from "react-router-dom";
import { useTranslation } from "react-i18next";
import { AlertVariant, Button, ButtonVariant } from "@patternfly/react-core";

import { useAdminClient, useFetch } from "../context/auth/AdminClient";
import type RoleRepresentation from "@keycloak/keycloak-admin-client/lib/defs/roleRepresentation";
import { ListEmptyState } from "../components/list-empty-state/ListEmptyState";
import { KeycloakSpinner } from "../components/keycloak-spinner/KeycloakSpinner";
import { KeycloakDataTable } from "../components/table-toolbar/KeycloakDataTable";
import { useAlerts } from "../components/alert/Alerts";
import { useConfirmDialog } from "../components/confirm-dialog/ConfirmDialog";
import { emptyFormatter, upperCaseFormatter } from "../util";
import { useRealm } from "../context/realm-context/RealmContext";
import type RealmRepresentation from "@keycloak/keycloak-admin-client/lib/defs/realmRepresentation";
import { HelpItem } from "../components/help-enabler/HelpItem";
import { ClientParams, ClientRoute } from "../clients/routes/Client";
import { toClientRole } from "./routes/ClientRole";
import { toRealmRole } from "./routes/RealmRole";
import { toRealmSettings } from "../realm-settings/routes/RealmSettings";

import "./RealmRolesSection.css";

type RolesListProps = {
  paginated?: boolean;
  parentRoleId?: string;
  messageBundle?: string;
  loader?: (
    first?: number,
    max?: number,
    search?: string
  ) => Promise<RoleRepresentation[]>;
};

type RoleLinkProps = {
  role: RoleRepresentation;
};

const RoleLink: FunctionComponent<RoleLinkProps> = ({ children, role }) => {
  const { realm } = useRealm();
  const clientRouteMatch = useRouteMatch<ClientParams>(ClientRoute.path);
  const to = clientRouteMatch
    ? toClientRole({ ...clientRouteMatch.params, id: role.id!, tab: "details" })
    : toRealmRole({ realm, id: role.id!, tab: "details" });

  return <Link to={to}>{children}</Link>;
};

export const RolesList = ({
  loader,
  paginated = true,
  parentRoleId,
  messageBundle = "roles",
}: RolesListProps) => {
  const { t } = useTranslation(messageBundle);
  const history = useHistory();
  const adminClient = useAdminClient();
  const { addAlert, addError } = useAlerts();
  const { url } = useRouteMatch();
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
      <RoleLink role={role}>{role.name}</RoleLink>
    ) : (
      <>
        <Link
          to={toRealmSettings({ realm: realmName, tab: "userRegistration" })}
        >
          {role.name}{" "}
        </Link>
        <HelpItem
          helpText={`${messageBundle}:defaultRole`}
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

  const goToCreate = () => history.push(`${url}/add-role`);

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
        toolbarItem={<Button onClick={goToCreate}>{t("createRole")}</Button>}
        actions={[
          {
            title: t("common:delete"),
            onRowClick: (role) => {
              setSelectedRole(role);
              if (role.name === realm!.defaultRole!.name) {
                addAlert(t("defaultRoleDeleteError"), AlertVariant.danger);
              } else toggleDeleteDialog();
            },
          },
        ]}
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
            instructions={t("noRolesInstructions")}
            primaryActionText={t("createRole")}
            onPrimaryAction={goToCreate}
          />
        }
      />
    </>
  );
};
