import React, { useState } from "react";
import { Link } from "react-router-dom";
import { Trans, useTranslation } from "react-i18next";
import { AlertVariant, PageSection } from "@patternfly/react-core";
import { InfoCircleIcon } from "@patternfly/react-icons";

import type UserRepresentation from "@keycloak/keycloak-admin-client/lib/defs/userRepresentation";
import type { RoleMappingPayload } from "@keycloak/keycloak-admin-client/lib/defs/roleRepresentation";
import type ClientRepresentation from "@keycloak/keycloak-admin-client/lib/defs/clientRepresentation";
import { useAdminClient, useFetch } from "../../context/auth/AdminClient";
import { useAlerts } from "../../components/alert/Alerts";
import {
  mapRoles,
  RoleMapping,
  Row,
} from "../../components/role-mapping/RoleMapping";
import { KeycloakSpinner } from "../../components/keycloak-spinner/KeycloakSpinner";
import { toUser } from "../../user/routes/User";
import { useRealm } from "../../context/realm-context/RealmContext";
import { useAccess } from "../../context/access/Access";

import "./service-account.css";

type ServiceAccountProps = {
  client: ClientRepresentation;
};

export const ServiceAccount = ({ client }: ServiceAccountProps) => {
  const { t } = useTranslation("clients");
  const adminClient = useAdminClient();
  const { addAlert, addError } = useAlerts();
  const { realm } = useRealm();

  const [hide, setHide] = useState(false);
  const [serviceAccount, setServiceAccount] = useState<UserRepresentation>();

  const { hasAccess } = useAccess();
  const hasManageClients = hasAccess("manage-clients");

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
      await adminClient.users.addRealmRoleMappings({
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
      addError("clients:roleMappingUpdatedError", error);
    }
  };
  return serviceAccount ? (
    <>
      <PageSection className="pf-u-pb-0">
        <InfoCircleIcon className="pf-c-alert__icon keycloak--service-account--info-text" />
        <span className="pf-u-pl-sm">
          <Trans i18nKey="clients-help:manageServiceAccountUser">
            {""}
            <Link
              to={toUser({ realm, id: serviceAccount.id!, tab: "settings" })}
            >
              {{ link: serviceAccount.username }}
            </Link>
          </Trans>
        </span>
      </PageSection>
      <RoleMapping
        name={client.clientId!}
        id={serviceAccount.id!}
        type="users"
        isManager={hasManageClients || client.access?.configure}
        loader={loader}
        save={assignRoles}
        onHideRolesToggle={() => setHide(!hide)}
      />
    </>
  ) : (
    <KeycloakSpinner />
  );
};
