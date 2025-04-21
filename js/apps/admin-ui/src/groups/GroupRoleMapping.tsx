import type { RoleMappingPayload } from "@keycloak/keycloak-admin-client/lib/defs/roleRepresentation";
import { AlertVariant } from "@patternfly/react-core";
import { useTranslation } from "react-i18next";
import { useAdminClient } from "../admin-client";
import { useAlerts } from "@keycloak/keycloak-ui-shared";
import { RoleMapping, Row } from "../components/role-mapping/RoleMapping";

type GroupRoleMappingProps = {
  id: string;
  name: string;
};

export const GroupRoleMapping = ({ id, name }: GroupRoleMappingProps) => {
  const { adminClient } = useAdminClient();

  const { t } = useTranslation();
  const { addAlert, addError } = useAlerts();

  const assignRoles = async (rows: Row[]) => {
    try {
      const realmRoles = rows
        .filter((row) => row.client === undefined)
        .map((row) => row.role as RoleMappingPayload)
        .flat();
      await adminClient.groups.addRealmRoleMappings({
        id,
        roles: realmRoles,
      });
      await Promise.all(
        rows
          .filter((row) => row.client !== undefined)
          .map((row) =>
            adminClient.groups.addClientRoleMappings({
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

  return <RoleMapping name={name} id={id} type="groups" save={assignRoles} />;
};
