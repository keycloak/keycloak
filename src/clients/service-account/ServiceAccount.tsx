import React, { useContext, useState } from "react";
import { useTranslation } from "react-i18next";
import { AlertVariant } from "@patternfly/react-core";

import type RoleRepresentation from "keycloak-admin/lib/defs/roleRepresentation";
import type { RoleMappingPayload } from "keycloak-admin/lib/defs/roleRepresentation";
import { useAdminClient } from "../../context/auth/AdminClient";
import { RealmContext } from "../../context/realm-context/RealmContext";
import { useAlerts } from "../../components/alert/Alerts";
import {
  CompositeRole,
  RoleMapping,
  Row,
} from "../../components/role-mapping/RoleMapping";

type ServiceAccountProps = {
  clientId: string;
};

export const ServiceAccount = ({ clientId }: ServiceAccountProps) => {
  const { t } = useTranslation("clients");
  const adminClient = useAdminClient();
  const { realm } = useContext(RealmContext);
  const { addAlert } = useAlerts();

  const [hide, setHide] = useState(false);
  const [serviceAccountId, setServiceAccountId] = useState("");
  const [name, setName] = useState("");

  const loader = async () => {
    const serviceAccount = await adminClient.clients.getServiceAccountUser({
      id: clientId,
    });
    setServiceAccountId(serviceAccount.id!);
    const effectiveRoles = await adminClient.users.listCompositeRealmRoleMappings(
      { id: serviceAccount.id! }
    );
    const assignedRoles = await adminClient.users.listRealmRoleMappings({
      id: serviceAccount.id!,
    });

    const clients = await adminClient.clients.find();
    setName(clients.find((c) => c.id === clientId)?.clientId!);
    const clientRoles = (
      await Promise.all(
        clients.map(async (client) => {
          return {
            client,
            roles: await adminClient.users.listClientRoleMappings({
              id: serviceAccount.id!,
              clientUniqueId: client.id!,
            }),
          };
        })
      )
    ).filter((rows) => rows.roles.length > 0);

    const findClient = (role: RoleRepresentation) => {
      const row = clientRoles.filter((row) =>
        row.roles.find((r) => r.id === role.id)
      )[0];
      return row ? row.client : undefined;
    };

    const clientRolesFlat = clientRoles.map((row) => row.roles).flat();

    const addInherentData = await (async () =>
      Promise.all(
        effectiveRoles.map(async (role) => {
          const compositeRoles = await adminClient.roles.getCompositeRolesForRealm(
            { realm, id: role.id! }
          );
          return compositeRoles.length > 0
            ? compositeRoles.map((r) => {
                return { ...r, parent: role };
              })
            : { ...role, parent: undefined };
        })
      ))();
    const uniqueRolesWithParent = addInherentData
      .flat()
      .filter(
        (role, index, array) =>
          array.findIndex((r) => r.id === role.id) === index
      );
    return ([
      ...(hide ? assignedRoles : uniqueRolesWithParent),
      ...clientRolesFlat,
    ] as CompositeRole[])
      .sort((r1, r2) => r1.name!.localeCompare(r2.name!))
      .map((role) => {
        return {
          client: findClient(role),
          role,
        } as Row;
      });
  };

  const assignRoles = async (rows: Row[]) => {
    try {
      const realmRoles = rows
        .filter((row) => row.client === undefined)
        .map((row) => row.role as RoleMappingPayload)
        .flat();
      adminClient.users.addRealmRoleMappings({
        id: serviceAccountId,
        roles: realmRoles,
      });
      await Promise.all(
        rows
          .filter((row) => row.client !== undefined)
          .map((row) =>
            adminClient.users.addClientRoleMappings({
              id: serviceAccountId,
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
    <RoleMapping
      name={name}
      id={serviceAccountId}
      type={"service-account"}
      loader={loader}
      save={assignRoles}
      onHideRolesToggle={() => setHide(!hide)}
    />
  );
};
