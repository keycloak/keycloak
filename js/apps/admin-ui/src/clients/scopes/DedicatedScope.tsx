import type ClientRepresentation from "@keycloak/keycloak-admin-client/lib/defs/clientRepresentation";
import type { RoleMappingPayload } from "@keycloak/keycloak-admin-client/lib/defs/roleRepresentation";
import {
  AlertVariant,
  Divider,
  FormGroup,
  PageSection,
  Switch,
} from "@patternfly/react-core";
import { useState } from "react";
import { useTranslation } from "react-i18next";
import { HelpItem } from "@keycloak/keycloak-ui-shared";
import { useAdminClient } from "../../admin-client";
import { useAlerts } from "@keycloak/keycloak-ui-shared";
import { FormAccess } from "../../components/form/FormAccess";
import { RoleMapping, Row } from "../../components/role-mapping/RoleMapping";
import { useAccess } from "../../context/access/Access";

type DedicatedScopeProps = {
  client: ClientRepresentation;
};

export const DedicatedScope = ({
  client: initialClient,
}: DedicatedScopeProps) => {
  const { adminClient } = useAdminClient();

  const { t } = useTranslation();
  const { addAlert, addError } = useAlerts();

  const [client, setClient] = useState<ClientRepresentation>(initialClient);

  const { hasAccess } = useAccess();
  const isManager = hasAccess("manage-clients") || client.access?.manage;

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
          realmRoles,
        ),
        ...rows
          .filter((row) => row.client !== undefined)
          .map((row) =>
            adminClient.clients.addClientScopeMappings(
              {
                id: client.id!,
                client: row.client!.id!,
              },
              [row.role as RoleMappingPayload],
            ),
          ),
      ]);

      addAlert(t("clientScopeSuccess"), AlertVariant.success);
    } catch (error) {
      addError("clientScopeError", error);
    }
  };

  const update = async () => {
    const newClient = { ...client, fullScopeAllowed: !client.fullScopeAllowed };
    try {
      await adminClient.clients.update({ id: client.id! }, newClient);
      addAlert(t("clientScopeSuccess"), AlertVariant.success);
      setClient(newClient);
    } catch (error) {
      addError("clientScopeError", error);
    }
  };

  return (
    <PageSection>
      <FormAccess
        role="manage-clients"
        fineGrainedAccess={client.access?.manage}
        isHorizontal
      >
        <FormGroup
          hasNoPaddingTop
          label={t("fullScopeAllowed")}
          labelIcon={
            <HelpItem
              helpText={t("fullScopeAllowedHelp")}
              fieldLabelId="fullScopeAllowed"
            />
          }
          fieldId="fullScopeAllowed"
        >
          <Switch
            id="fullScopeAllowed"
            label={t("on")}
            labelOff={t("off")}
            isChecked={client.fullScopeAllowed}
            onChange={update}
            aria-label={t("fullScopeAllowed")}
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
            save={assignRoles}
            isManager={isManager}
          />
        </>
      )}
    </PageSection>
  );
};
