import React, { FunctionComponent, useState } from "react";
import { Link, useHistory, useRouteMatch } from "react-router-dom";
import { useTranslation } from "react-i18next";
import { AlertVariant, Button, ButtonVariant } from "@patternfly/react-core";

import { useAdminClient, useFetch } from "../context/auth/AdminClient";
import type RoleRepresentation from "keycloak-admin/lib/defs/roleRepresentation";
import { ListEmptyState } from "../components/list-empty-state/ListEmptyState";
import { KeycloakDataTable } from "../components/table-toolbar/KeycloakDataTable";
import { useAlerts } from "../components/alert/Alerts";
import { useConfirmDialog } from "../components/confirm-dialog/ConfirmDialog";
import { emptyFormatter, upperCaseFormatter } from "../util";
import { useRealm } from "../context/realm-context/RealmContext";
import type RealmRepresentation from "keycloak-admin/lib/defs/realmRepresentation";
import { HelpItem } from "../components/help-enabler/HelpItem";
import { toRealmRole } from "./routes/RealmRole";

import "./RealmRolesSection.css";

type myRealmRepresentation = RealmRepresentation & {
  defaultRole?: {
    id: string;
    name: string;
  };
};

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

  return (
    <Link key={role.id} to={toRealmRole({ realm, id: role.id! })}>
      {children}
    </Link>
  );
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
  const [realm, setRealm] = useState<myRealmRepresentation>();

  const [selectedRole, setSelectedRole] = useState<RoleRepresentation>();

  useFetch(
    () => adminClient.realms.findOne({ realm: realmName }),
    (realm) => {
      setRealm(realm);
    },
    []
  );

  const RoleDetailLink = (role: RoleRepresentation) => (
    <>
      <RoleLink role={role}>{role.name}</RoleLink>
      {role.name?.includes("default-role") ? (
        <HelpItem
          helpText={t("defaultRole")}
          forLabel={t("defaultRole")}
          forID="kc-defaultRole"
          id="default-role-help-icon"
        />
      ) : (
        ""
      )}
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
          <>
            <Button onClick={goToCreate}>{t("createRole")}</Button>
          </>
        }
        actions={[
          {
            title: t("common:delete"),
            onRowClick: (role) => {
              setSelectedRole(role as RoleRepresentation);
              if (
                (role as RoleRepresentation).name === realm!.defaultRole!.name
              ) {
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
