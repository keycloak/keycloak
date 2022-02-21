import React, { useState } from "react";
import { useTranslation } from "react-i18next";
import {
  AlertVariant,
  Divider,
  FormGroup,
  PageSection,
  Switch,
} from "@patternfly/react-core";

import type ClientRepresentation from "@keycloak/keycloak-admin-client/lib/defs/clientRepresentation";
import { FormAccess } from "../../components/form-access/FormAccess";
import { HelpItem } from "../../components/help-enabler/HelpItem";
import { useAdminClient } from "../../context/auth/AdminClient";
import { useAlerts } from "../../components/alert/Alerts";
import {
  mapRoles,
  RoleMapping,
  Row,
} from "../../components/role-mapping/RoleMapping";
import type { RoleMappingPayload } from "@keycloak/keycloak-admin-client/lib/defs/roleRepresentation";
import useToggle from "../../utils/useToggle";

type DedicatedScopeProps = {
  client: ClientRepresentation;
};

export const DedicatedScope = ({
  client: initialClient,
}: DedicatedScopeProps) => {
  const { t } = useTranslation("clients");
  const adminClient = useAdminClient();
  const { addAlert, addError } = useAlerts();

  const [client, setClient] = useState<ClientRepresentation>(initialClient);
  const [hide, toggle] = useToggle();

  const loader = async () => {
    const [clients, assignedRoles, effectiveRoles] = await Promise.all([
      adminClient.clients.find(),
      adminClient.clients
        .listRealmScopeMappings({ id: client.id! })
        .then((roles) => roles.map((role) => ({ role }))),
      adminClient.clients
        .listCompositeRealmScopeMappings({ id: client.id! })
        .then((roles) => roles.map((role) => ({ role }))),
    ]);

    const clientRoles = (
      await Promise.all(
        clients.map(async ({ id }) => {
          const [clientAssignedRoles, clientEffectiveRoles] = await Promise.all(
            [
              adminClient.clients
                .listClientScopeMappings({
                  id: client.id!,
                  client: id!,
                })
                .then((roles) => roles.map((role) => ({ role, client }))),
              adminClient.clients
                .listCompositeClientScopeMappings({
                  id: client.id!,
                  client: id!,
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
      await Promise.all([
        adminClient.clients.addRealmScopeMappings(
          {
            id: client.id!,
          },
          realmRoles
        ),
        ...rows
          .filter((row) => row.client !== undefined)
          .map((row) =>
            adminClient.clients.addClientScopeMappings(
              {
                id: client.id!,
                client: row.client!.id!,
              },
              [row.role as RoleMappingPayload]
            )
          ),
      ]);

      addAlert(t("clientScopeSuccess"), AlertVariant.success);
    } catch (error) {
      addError("clients:clientScopeError", error);
    }
  };

  const update = async () => {
    const newClient = { ...client, fullScopeAllowed: !client.fullScopeAllowed };
    try {
      await adminClient.clients.update({ id: client.id! }, newClient);
      addAlert(t("clientScopeSuccess"), AlertVariant.success);
      setClient(newClient);
    } catch (error) {
      addError("clients:clientScopeError", error);
    }
  };

  return (
    <PageSection>
      <FormAccess role="manage-clients" isHorizontal>
        <FormGroup
          hasNoPaddingTop
          label={t("fullScopeAllowed")}
          labelIcon={
            <HelpItem
              helpText="clients-help:fullScopeAllowed"
              fieldLabelId="clients:fullScopeAllowed"
            />
          }
          fieldId="fullScopeAllowed"
        >
          <Switch
            id="fullScopeAllowed"
            label={t("common:on")}
            labelOff={t("common:off")}
            isChecked={client.fullScopeAllowed}
            onChange={update}
          />
        </FormGroup>
      </FormAccess>
      {!client.fullScopeAllowed && (
        <>
          <Divider />
          <RoleMapping
            name={client.clientId!}
            id={client.id!}
            type="clients"
            loader={loader}
            save={assignRoles}
            onHideRolesToggle={toggle}
          />
        </>
      )}
    </PageSection>
  );
};
