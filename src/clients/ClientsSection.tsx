import React, { useEffect, useState } from "react";
import { Link, useHistory, useRouteMatch } from "react-router-dom";
import { useTranslation } from "react-i18next";
import {
  AlertVariant,
  Badge,
  Button,
  ButtonVariant,
  PageSection,
} from "@patternfly/react-core";

import { ViewHeader } from "../components/view-header/ViewHeader";
import { useAdminClient } from "../context/auth/AdminClient";
import { KeycloakDataTable } from "../components/table-toolbar/KeycloakDataTable";
import { emptyFormatter, exportClient, getBaseUrl } from "../util";
import { useAlerts } from "../components/alert/Alerts";
import ClientRepresentation from "keycloak-admin/lib/defs/clientRepresentation";
import { formattedLinkTableCell } from "../components/external-link/FormattedLink";
import { useConfirmDialog } from "../components/confirm-dialog/ConfirmDialog";

export const ClientsSection = () => {
  const { t } = useTranslation("clients");
  const { addAlert } = useAlerts();
  const history = useHistory();
  const { url } = useRouteMatch();

  const adminClient = useAdminClient();
  const baseUrl = getBaseUrl(adminClient);

  const [key, setKey] = useState(0);
  const refresh = () => setKey(new Date().getTime());
  const [selectedClient, setSelectedClient] = useState<ClientRepresentation>();

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

  useEffect(refresh, [selectedClient]);

  const [toggleDeleteDialog, DeleteConfirm] = useConfirmDialog({
    titleKey: t("clientDelete", { clientId: selectedClient?.clientId }),
    messageKey: "clients:clientDeleteConfirm",
    continueButtonLabel: "common:delete",
    continueButtonVariant: ButtonVariant.danger,
    onConfirm: async () => {
      try {
        await adminClient.clients.del({
          id: selectedClient!.id!,
        });
        addAlert(t("clientDeletedSuccess"), AlertVariant.success);
        setSelectedClient(undefined);
      } catch (error) {
        addAlert(t("clientDeleteError", { error }), AlertVariant.danger);
      }
    },
  });

  const ClientDetailLink = (client: ClientRepresentation) => (
    <>
      <Link key={client.id} to={`${url}/${client.id}`}>
        {client.clientId}
        {!client.enabled && (
          <Badge isRead className="pf-u-ml-sm">
            Disabled
          </Badge>
        )}
      </Link>
    </>
  );

  return (
    <>
      <ViewHeader
        titleKey="clients:clientList"
        subKey="clients:clientsExplain"
      />
      <PageSection variant="light" className="pf-u-p-0">
        <DeleteConfirm />
        <KeycloakDataTable
          key={key}
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
              onRowClick: (client) => {
                setSelectedClient(client);
                toggleDeleteDialog();
              },
            },
          ]}
          columns={[
            {
              name: "clientId",
              displayKey: "clients:clientID",
              cellRenderer: ClientDetailLink,
            },
            { name: "protocol", displayKey: "common:type" },
            {
              name: "description",
              displayKey: "common:description",
              cellFormatters: [emptyFormatter()],
            },
            {
              name: "baseUrl",
              displayKey: "clients:homeURL",
              cellFormatters: [formattedLinkTableCell(), emptyFormatter()],
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
