import React, { useState } from "react";
import { useTranslation } from "react-i18next";
import { AlertVariant } from "@patternfly/react-core";

import type { RoleMappingPayload } from "@keycloak/keycloak-admin-client/lib/defs/roleRepresentation";
import { useAdminClient } from "../context/auth/AdminClient";
import { useAlerts } from "../components/alert/Alerts";
import {
  mapRoles,
  RoleMapping,
  Row,
} from "../components/role-mapping/RoleMapping";

type UserRoleMappingProps = {
  id: string;
  name: string;
};

export const UserRoleMapping = ({ id, name }: UserRoleMappingProps) => {
  const { t } = useTranslation("clients");
  const adminClient = useAdminClient();
  const { addAlert, addError } = useAlerts();

  const [hide, setHide] = useState(false);

  const loader = async () => {
    const [assignedRoles, effectiveRoles] = await Promise.all([
      adminClient.users
        .listRealmRoleMappings({ id })
        .then((roles) => roles.map((role) => ({ role }))),
      adminClient.users
        .listCompositeRealmRoleMappings({ id })
        .then((roles) => roles.map((role) => ({ role }))),
    ]);

    const clients = await adminClient.clients.find();
    const clientRoles = (
      await Promise.all(
        clients.map(async (client) => {
          const [clientAssignedRoles, clientEffectiveRoles] = await Promise.all(
            [
              adminClient.users
                .listClientRoleMappings({
                  id,
                  clientUniqueId: client.id!,
                })
                .then((roles) => roles.map((role) => ({ role, client }))),
              adminClient.users
                .listCompositeClientRoleMappings({
                  id,
                  clientUniqueId: client.id!,
                })
                .then((roles) => roles.map((role) => ({ role, client }))),
            ]
          );
          return mapRoles(clientAssignedRoles, clientEffectiveRoles, hide);
        })
      )
    ).flat();

    return [...mapRoles(assignedRoles, effectiveRoles, hide), ...clientRoles];
  };

  const assignRoles = async (rows: Row[]) => {
    try {
      const realmRoles = rows
        .filter((row) => row.client === undefined)
        .map((row) => row.role as RoleMappingPayload)
        .flat();
      await adminClient.users.addRealmRoleMappings({
        id,
        roles: realmRoles,
      });
      await Promise.all(
        rows
          .filter((row) => row.client !== undefined)
          .map((row) =>
            adminClient.users.addClientRoleMappings({
              id,
              clientUniqueId: row.client!.id!,
              roles: [row.role as RoleMappingPayload],
            })
          )
      );
      addAlert(t("roleMappingUpdatedSuccess"), AlertVariant.success);
    } catch (error) {
      addError("clients:roleMappingUpdatedError", error);
    }
  };

  return (
    <RoleMapping
      name={name}
      id={id}
      type="service-account"
      loader={loader}
      save={assignRoles}
      onHideRolesToggle={() => setHide(!hide)}
    />
  );
};
