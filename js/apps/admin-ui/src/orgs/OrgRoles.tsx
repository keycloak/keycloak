import { useState } from "react";
import {
  AlertVariant,
  Badge,
  Button,
  ToolbarItem,
} from "@patternfly/react-core";

import type { OrgRepresentation } from "./routes";
import { KeycloakDataTable } from "../components/table-toolbar/KeycloakDataTable";
import useOrgFetcher from "./useOrgFetcher";
import { useRealm } from "../context/realm-context/RealmContext";
import { ListEmptyState } from "../components/list-empty-state/ListEmptyState";
import type RoleRepresentation from "@keycloak/keycloak-admin-client/lib/defs/roleRepresentation";
import { useTranslation } from "react-i18next";
import { useAlerts } from "../components/alert/Alerts";
import { emptyFormatter } from "../util";
import type { IRowData } from "@patternfly/react-table";
import { NewOrgRoleModal } from "./modals/NewOrgRoleModal";
import { EditOrgRoleModal } from "./modals/EditOrgRoleModal";

type OrgRolesProps = {
  org: OrgRepresentation;
};

const defaultRoles = [
  "view-organization",
  "manage-organization",
  "view-clients",
  "manage-clients",
  "view-members",
  "manage-members",
  "view-roles",
  "manage-roles",
  "view-invitations",
  "manage-invitations",
  "view-identity-providers",
  "manage-identity-providers",
];

export default function OrgRoles({ org }: OrgRolesProps) {
  // Data Table
  const { realm } = useRealm();
  const { getRolesForOrg, deleteRoleFromOrg } = useOrgFetcher(realm);
  const { t } = useTranslation();
  const { addAlert } = useAlerts();

  const [key, setKey] = useState(0);
  const refresh = () => setKey(new Date().getTime());

  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  const loader = async (first?: number, max?: number, search?: string) => {
    const roles = (await getRolesForOrg(org.id)) as RoleRepresentation[];
    // sort alphabetically
    // sort by the user defined (and editable) vs default ones
    return (
      roles
        // @ts-ignore
        .sort((a, b) => a.name - b.name)
        .sort(
          (a, b) =>
            defaultRoles.indexOf(a.name!) - defaultRoles.indexOf(b.name!),
        )
    );
  };

  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  const [createRoleModalVisibility, setCreateRoleModalVisibility] =
    useState(false);
  const [editRoleModalVisibility, setEditRoleModalVisibility] = useState<
    RoleRepresentation | boolean
  >(false);

  const RoleName = (role: RoleRepresentation) =>
    !defaultRoles.includes(role.name!) ? (
      <Button
        key={role.id}
        variant="link"
        onClick={() => setEditRoleModalVisibility(role)}
        isSmall
        isInline
      >
        <Badge isRead className="keycloak-admin--role-mapping__client-name">
          defined
        </Badge>
        {role.name}
      </Button>
    ) : (
      <div>
        <Badge
          key={role.id}
          isRead
          className="keycloak-admin--role-mapping__client-name"
        >
          default
        </Badge>
        {role.name}
      </div>
    );

  // TODO: change this to a confirm dialog for the built in option
  async function deleteRole(role: RoleRepresentation): Promise<boolean> {
    if (
      !confirm(
        `Confirm you wish to remove role: ${role.name}. This cannot be undone.`,
      )
    ) {
      return Promise.resolve(true);
    }
    const resp = await deleteRoleFromOrg(org.id, role);
    if (resp.success) {
      addAlert(resp.message, AlertVariant.success);
      refresh();
    } else {
      addAlert(
        `${t("removeRoleFromOrgFail")} ${resp.message}`,
        AlertVariant.danger,
      );
    }

    return Promise.resolve(true);
  }

  return (
    <>
      <KeycloakDataTable
        data-testid="roles-org-table"
        key={`${org.id}${key}`}
        loader={loader}
        ariaLabelKey="invitations"
        isRowDisabled={(value) => {
          return defaultRoles.includes(value.name!);
        }}
        toolbarItem={
          <ToolbarItem>
            <Button
              data-testid="addInvitation"
              variant="primary"
              onClick={() => setCreateRoleModalVisibility(true)}
            >
              Create Role
            </Button>
          </ToolbarItem>
        }
        actionResolver={(rowData: IRowData) => [
          {
            title: t("editRole"),
            onClick: () => setEditRoleModalVisibility(rowData.data),
          },
          {
            title: t("deleteRole"),
            onClick: () => deleteRole(rowData.data),
          },
        ]}
        columns={[
          {
            name: "name",
            displayKey: "name",
            cellRenderer: RoleName,
          },
          {
            name: "description",
            displayKey: "description",
            cellFormatters: [emptyFormatter()],
          },
        ]}
        emptyState={
          <ListEmptyState
            message="No Roles Found"
            instructions="There are currently no roles available for this organization."
            primaryActionText="Create Role"
            onPrimaryAction={() => setCreateRoleModalVisibility(true)}
          />
        }
      />
      {createRoleModalVisibility && (
        <NewOrgRoleModal
          orgId={org.id}
          handleModalToggle={() => setCreateRoleModalVisibility(false)}
          refresh={refresh}
        />
      )}
      {editRoleModalVisibility && (
        <EditOrgRoleModal
          orgId={org.id}
          role={editRoleModalVisibility as RoleRepresentation}
          handleModalToggle={() => setEditRoleModalVisibility(false)}
          refresh={refresh}
        />
      )}
    </>
  );
}
