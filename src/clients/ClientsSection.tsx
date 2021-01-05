import React from "react";
import { Link, useHistory, useRouteMatch } from "react-router-dom";
import { useTranslation } from "react-i18next";
import {
  AlertVariant,
  Badge,
  Button,
  PageSection,
} from "@patternfly/react-core";

import { ViewHeader } from "../components/view-header/ViewHeader";
import { useAdminClient } from "../context/auth/AdminClient";
import { KeycloakDataTable } from "../components/table-toolbar/KeycloakDataTable";
import { IFormatter, IFormatterValueType } from "@patternfly/react-table";
import { exportClient } from "../util";
import { useAlerts } from "../components/alert/Alerts";
import ClientRepresentation from "keycloak-admin/lib/defs/clientRepresentation";
import { ExternalLink } from "../components/external-link/ExternalLink";

export const ClientsSection = () => {
  const { t } = useTranslation("clients");
  const { addAlert } = useAlerts();
  const history = useHistory();
  const { url } = useRouteMatch();

  const adminClient = useAdminClient();
  const baseUrl = adminClient.keycloak
    ? adminClient.keycloak.authServerUrl!
    : adminClient.baseUrl + "/";

  const loader = async (first?: number, max?: number, search?: string) => {
    const params: { [name: string]: string | number } = {
      first: first!,
      max: max!,
    };
    if (search) {
      params.clientId = search;
      params.search = "true";
    }
    return await adminClient.clients.find({ ...params });
  };

  const emptyFormatter = (): IFormatter => (data?: IFormatterValueType) => {
    return data ? data : "—";
  };

  const externalLink = (): IFormatter => (data?: IFormatterValueType) => {
    return (data ? (
      <ExternalLink href={data.toString()} />
    ) : undefined) as object;
  };

  const ClientDetailLink = (client: ClientRepresentation) => (
    <>
      <Link key={client.id} to={`${url}/${client.id}`}>
        {client.clientId}
        {!client.enabled && <Badge isRead>Disabled</Badge>}
      </Link>
    </>
  );
  return (
    <>
      <ViewHeader
        titleKey="clients:clientList"
        subKey="clients:clientsExplain"
      />
      <PageSection variant="light">
        <KeycloakDataTable
          loader={loader}
          isPaginated
          ariaLabelKey="clients:clientList"
          searchPlaceholderKey="clients:searchForClient"
          toolbarItem={
            <>
              <Button onClick={() => history.push(`${url}/add-client`)}>
                {t("createClient")}
              </Button>
              <Button
                onClick={() => history.push(`${url}/import-client`)}
                variant="link"
              >
                {t("importClient")}
              </Button>
            </>
          }
          actions={[
            {
              title: t("common:export"),
              onRowClick: (client) => {
                exportClient(client);
              },
            },
            {
              title: t("common:delete"),
              onRowClick: async (client) => {
                try {
                  await adminClient.clients.del({
                    id: client.id!,
                  });
                  addAlert(t("clientDeletedSuccess"), AlertVariant.success);
                  return true;
                } catch (error) {
                  addAlert(
                    t("clientDeleteError", { error }),
                    AlertVariant.danger
                  );
                }
              },
            },
          ]}
          columns={[
            {
              name: "clientId",
              displayKey: "clients:clientID",
              cellRenderer: ClientDetailLink,
            },
            { name: "protocol", displayKey: "clients:type" },
            {
              name: "description",
              displayKey: "clients:description",
              cellFormatters: [emptyFormatter()],
            },
            {
              name: "baseUrl",
              displayKey: "clients:homeURL",
              cellFormatters: [externalLink(), emptyFormatter()],
              cellRenderer: (client) => {
                if (client.rootUrl) {
                  if (
                    !client.rootUrl.startsWith("http") ||
                    client.rootUrl.indexOf("$") !== -1
                  ) {
                    client.rootUrl =
                      client.rootUrl
                        .replace("${authBaseUrl}", baseUrl)
                        .replace("${authAdminUrl}", baseUrl) +
                      (client.baseUrl ? client.baseUrl.substr(1) : "");
                  }
                }
                return client.rootUrl;
              },
            },
          ]}
        />
      </PageSection>
    </>
  );
};
