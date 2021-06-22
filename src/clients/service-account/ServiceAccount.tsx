import React, { useState } from "react";
import { useTranslation } from "react-i18next";
import { AlertVariant } from "@patternfly/react-core";

import type UserRepresentation from "keycloak-admin/lib/defs/userRepresentation";
import type { RoleMappingPayload } from "keycloak-admin/lib/defs/roleRepresentation";
import type ClientRepresentation from "keycloak-admin/lib/defs/clientRepresentation";
import { useAdminClient, useFetch } from "../../context/auth/AdminClient";
import { useAlerts } from "../../components/alert/Alerts";
import {
  mapRoles,
  RoleMapping,
  Row,
} from "../../components/role-mapping/RoleMapping";

type ServiceAccountProps = {
  client: ClientRepresentation;
};

export const ServiceAccount = ({ client }: ServiceAccountProps) => {
  const { t } = useTranslation("clients");
  const adminClient = useAdminClient();
  const { addAlert } = useAlerts();

  const [hide, setHide] = useState(false);
  const [serviceAccount, setServiceAccount] = useState<UserRepresentation>();

  useFetch(
    () =>
      adminClient.clients.getServiceAccountUser({
        id: client.id!,
      }),
    (serviceAccount) => setServiceAccount(serviceAccount),
    []
  );

  const loader = async () => {
    const serviceAccount = await adminClient.clients.getServiceAccountUser({
      id: client.id!,
    });
    const id = serviceAccount.id!;

    const assignedRoles = (
      await adminClient.users.listRealmRoleMappings({ id })
    ).map((role) => ({ role }));
    const effectiveRoles = (
      await adminClient.users.listCompositeRealmRoleMappings({ id })
    ).map((role) => ({ role }));

    const clients = await adminClient.clients.find();
    const clientRoles = (
      await Promise.all(
        clients.map(async (client) => {
          const clientAssignedRoles = (
            await adminClient.users.listClientRoleMappings({
              id,
              clientUniqueId: client.id!,
            })
          ).map((role) => ({ role, client }));
          const clientEffectiveRoles = (
            await adminClient.users.listCompositeClientRoleMappings({
              id,
              clientUniqueId: client.id!,
            })
          ).map((role) => ({ role, client }));
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
      adminClient.users.addRealmRoleMappings({
        id: serviceAccount?.id!,
        roles: realmRoles,
      });
      await Promise.all(
        rows
          .filter((row) => row.client !== undefined)
          .map((row) =>
            adminClient.users.addClientRoleMappings({
              id: serviceAccount?.id!,
              clientUniqueId: row.client!.id!,
              roles: [row.role as RoleMappingPayload],
            })
          )
      );
      addAlert(t("roleMappingUpdatedSuccess"), AlertVariant.success);
    } catch (error) {
      addAlert(
        t("roleMappingUpdatedError", {
          error: error.response?.data?.errorMessage || error,
        }),
        AlertVariant.danger
      );
    }
  };
  return (
    <>
      {serviceAccount && (
        <RoleMapping
          name={client.clientId!}
          id={serviceAccount.id!}
          type="service-account"
          loader={loader}
          save={assignRoles}
          onHideRolesToggle={() => setHide(!hide)}
        />
      )}
    </>
  );
};
