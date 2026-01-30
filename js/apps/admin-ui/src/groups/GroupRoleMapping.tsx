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
        .filter((row) => row.client === undefined)
        .map((row) => row.role as RoleMappingPayload)
        .flat();
      await groups.addRealmRoleMappings({
        id,
        roles: realmRoles,
      });
      await Promise.all(
        rows
          .filter((row) => row.client !== undefined)
          .map((row) =>
            groups.addClientRoleMappings({
              id,
              clientUniqueId: row.client!.id!,
              roles: [row.role as RoleMappingPayload],
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
    />
  );
};
