import type ClientRepresentation from "@keycloak/keycloak-admin-client/lib/defs/clientRepresentation";
import type { RoleMappingPayload } from "@keycloak/keycloak-admin-client/lib/defs/roleRepresentation";
import type UserRepresentation from "@keycloak/keycloak-admin-client/lib/defs/userRepresentation";
import { useAlerts, useFetch } from "@keycloak/keycloak-ui-shared";
import { AlertVariant, PageSection } from "@patternfly/react-core";
import { InfoCircleIcon } from "@patternfly/react-icons";
import { useState } from "react";
import { Trans, useTranslation } from "react-i18next";
import { Link } from "react-router-dom";
import { useAdminClient } from "../../admin-client";
import { KeycloakSpinner } from "@keycloak/keycloak-ui-shared";
import { RoleMapping, Row } from "../../components/role-mapping/RoleMapping";
import { useAccess } from "../../context/access/Access";
import { useRealm } from "../../context/realm-context/RealmContext";
import { toUser } from "../../user/routes/User";

import "./service-account.css";

type ServiceAccountProps = {
  client: ClientRepresentation;
};

export const ServiceAccount = ({ client }: ServiceAccountProps) => {
  const { adminClient } = useAdminClient();

  const { t } = useTranslation();
  const { addAlert, addError } = useAlerts();
  const { realm } = useRealm();

  const [serviceAccount, setServiceAccount] = useState<UserRepresentation>();

  const { hasAccess } = useAccess();
  const hasManageClients = hasAccess("manage-clients");

  useFetch(
    () =>
      adminClient.clients.getServiceAccountUser({
        id: client.id!,
      }),
    (serviceAccount) => setServiceAccount(serviceAccount),
    [],
  );

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
            }),
          ),
      );
      addAlert(t("roleMappingUpdatedSuccess"), AlertVariant.success);
    } catch (error) {
      addError("roleMappingUpdatedError", error);
    }
  };
  return serviceAccount ? (
    <>
      <PageSection className="pf-v5-u-pb-0">
        <InfoCircleIcon className="pf-v5-c-alert__icon keycloak--service-account--info-text" />
        <span className="pf-v5-u-pl-sm">
          <Trans i18nKey="manageServiceAccountUser">
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
        save={assignRoles}
      />
    </>
  ) : (
    <KeycloakSpinner />
  );
};
