import React, { useState } from "react";
import { useParams } from "react-router-dom";
import { useTranslation } from "react-i18next";
import {
  AlertVariant,
  PageSection,
  Spinner,
  Tab,
  TabTitleText,
} from "@patternfly/react-core";

import type ClientScopeRepresentation from "keycloak-admin/lib/defs/clientScopeRepresentation";
import { useAdminClient, useFetch } from "../../context/auth/AdminClient";
import { KeycloakTabs } from "../../components/keycloak-tabs/KeycloakTabs";
import { useAlerts } from "../../components/alert/Alerts";
import { ViewHeader } from "../../components/view-header/ViewHeader";
import { convertFormValuesToObject } from "../../util";
import { MapperList } from "../details/MapperList";
import { ScopeForm } from "../details/ScopeForm";
import { RoleMapping, Row } from "../../components/role-mapping/RoleMapping";
import type { RoleMappingPayload } from "keycloak-admin/lib/defs/roleRepresentation";

export const ClientScopeForm = () => {
  const { t } = useTranslation("client-scopes");
  const [clientScope, setClientScope] = useState<ClientScopeRepresentation>();
  const [hide, setHide] = useState(false);

  const adminClient = useAdminClient();
  const { id } = useParams<{ id: string }>();

  const { addAlert } = useAlerts();

  const [key, setKey] = useState(0);
  const refresh = () => setKey(new Date().getTime());

  useFetch(
    async () => {
      if (id) {
        return await adminClient.clientScopes.findOne({ id });
      }
    },
    (clientScope) => {
      setClientScope(clientScope);
    },
    [key, id]
  );

  const loader = async () => {
    const assignedRoles = hide
      ? await adminClient.clientScopes.listRealmScopeMappings({ id })
      : await adminClient.clientScopes.listCompositeRealmScopeMappings({ id });
    const clients = await adminClient.clients.find();

    const clientRoles = (
      await Promise.all(
        clients.map(async (client) => {
          const clientScope = hide
            ? await adminClient.clientScopes.listClientScopeMappings({
                id,
                client: client.id!,
              })
            : await adminClient.clientScopes.listCompositeClientScopeMappings({
                id,
                client: client.id!,
              });
          return clientScope.map((scope) => {
            return {
              client,
              role: scope,
            };
          });
        })
      )
    ).flat();

    return [
      ...assignedRoles.map((role) => {
        return {
          role,
        };
      }),
      ...clientRoles,
    ];
  };

  const save = async (clientScopes: ClientScopeRepresentation) => {
    try {
      clientScopes.attributes = convertFormValuesToObject(
        clientScopes.attributes!
      );

      if (id) {
        await adminClient.clientScopes.update({ id }, clientScopes);
      } else {
        await adminClient.clientScopes.create(clientScopes);
      }
      addAlert(t((id ? "update" : "create") + "Success"), AlertVariant.success);
    } catch (error) {
      addAlert(
        t((id ? "update" : "create") + "Error", { error }),
        AlertVariant.danger
      );
    }
  };

  const assignRoles = async (rows: Row[]) => {
    try {
      const realmRoles = rows
        .filter((row) => row.client === undefined)
        .map((row) => row.role as RoleMappingPayload)
        .flat();
      await adminClient.clientScopes.addRealmScopeMappings(
        {
          id,
        },
        realmRoles
      );
      await Promise.all(
        rows
          .filter((row) => row.client !== undefined)
          .map((row) =>
            adminClient.clientScopes.addClientScopeMappings(
              {
                id,
                client: row.client!.id!,
              },
              [row.role as RoleMappingPayload]
            )
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

  if (id && !clientScope) {
    return (
      <div className="pf-u-text-align-center">
        <Spinner />
      </div>
    );
  }

  return (
    <>
      <ViewHeader
        titleKey={
          clientScope ? clientScope.name! : "client-scopes:createClientScope"
        }
        subKey="client-scopes:clientScopeExplain"
        badge={clientScope ? clientScope.protocol : undefined}
        divider={!id}
      />

      <PageSection variant="light" className="pf-u-p-0">
        {!id && (
          <PageSection variant="light">
            <ScopeForm save={save} clientScope={{}} />
          </PageSection>
        )}
        {id && clientScope && (
          <KeycloakTabs isBox>
            <Tab
              eventKey="settings"
              title={<TabTitleText>{t("common:settings")}</TabTitleText>}
            >
              <PageSection variant="light">
                <ScopeForm save={save} clientScope={clientScope} />
              </PageSection>
            </Tab>
            <Tab
              eventKey="mappers"
              title={<TabTitleText>{t("common:mappers")}</TabTitleText>}
            >
              <MapperList clientScope={clientScope} refresh={refresh} />
            </Tab>
            <Tab
              data-testid="scopeTab"
              eventKey="scope"
              title={<TabTitleText>{t("scope")}</TabTitleText>}
            >
              <RoleMapping
                id={id}
                name={clientScope.name!}
                type={"client-scope"}
                loader={loader}
                save={assignRoles}
                onHideRolesToggle={() => setHide(!hide)}
              />
            </Tab>
          </KeycloakTabs>
        )}
      </PageSection>
    </>
  );
};
