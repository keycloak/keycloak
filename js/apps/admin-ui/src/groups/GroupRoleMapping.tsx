import type { RoleMappingPayload } from "@keycloak/keycloak-admin-client/lib/defs/roleRepresentation";
import { AlertVariant } from "@patternfly/react-core";
import { useTranslation } from "react-i18next";
import { useAlerts } from "@keycloak/keycloak-ui-shared";
import { RoleMapping, Row } from "../components/role-mapping/RoleMapping";
import { useGroupResource } from "../context/group-resource/GroupResourceContext";

type GroupRoleMappingProps = {
  id: string;
  name: string;
  canManageGroup: boolean;
};

const toRoleMappingPayload = (row: Row): RoleMappingPayload => {
  if (!row.role.id || !row.role.name) {
    throw new Error("Role mappings require both a role id and name");
  }
  return { id: row.role.id, name: row.role.name };
};

export const GroupRoleMapping = ({
  id,
  name,
  canManageGroup,
}: GroupRoleMappingProps) => {
  const groups = useGroupResource();

  const { t } = useTranslation();
  const { addAlert, addError } = useAlerts();

  const assignRoles = async (rows: Row[]) => {
    try {
      const realmRoles = rows
        .filter((row) => row.client === undefined && !row.org)
        .map(toRoleMappingPayload);
      if (realmRoles.length > 0) {
        await groups.addRealmRoleMappings({ id, roles: realmRoles });
      }

      const orgRoles = rows.filter((row) => row.org).map(toRoleMappingPayload);
      if (orgRoles.length > 0) {
        await groups.addOrganizationRoleMappings({ id, roles: orgRoles });
      }

      await Promise.all(
        rows
          .filter((row) => row.client !== undefined)
          .map((row) =>
            groups.addClientRoleMappings({
              id,
              clientUniqueId: row.client!.id!,
              roles: [toRoleMappingPayload(row)],
            }),
          ),
      );
      addAlert(t("roleMappingUpdatedSuccess"), AlertVariant.success);
    } catch (error) {
      addError("roleMappingUpdatedError", error);
    }
  };

  return (
    <RoleMapping
      isManager={canManageGroup}
      name={name}
      id={id}
      type="groups"
      save={assignRoles}
      groupsResource={groups}
      canMapOrganizationRoles={groups.isOrgGroups() && canManageGroup}
    />
  );
};
