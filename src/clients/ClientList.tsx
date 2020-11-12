import React from "react";
import { useTranslation } from "react-i18next";
import { Link } from "react-router-dom";
import {
  Table,
  TableBody,
  TableHeader,
  TableVariant,
  IFormatter,
  IFormatterValueType,
} from "@patternfly/react-table";
import { Badge, AlertVariant } from "@patternfly/react-core";
import ClientRepresentation from "keycloak-admin/lib/defs/clientRepresentation";

import { ExternalLink } from "../components/external-link/ExternalLink";
import { useAlerts } from "../components/alert/Alerts";
import { useAdminClient } from "../context/auth/AdminClient";
import { exportClient } from "../util";

type ClientListProps = {
  clients?: ClientRepresentation[];
  refresh: () => void;
  baseUrl: string;
};

const columns: (keyof ClientRepresentation)[] = [
  "clientId",
  "protocol",
  "description",
  "baseUrl",
];

export const ClientList = ({ baseUrl, clients, refresh }: ClientListProps) => {
  const { t } = useTranslation("clients");
  const adminClient = useAdminClient();
  const { addAlert } = useAlerts();

  const emptyFormatter = (): IFormatter => (data?: IFormatterValueType) => {
    return data ? data : "—";
  };

  const externalLink = (): IFormatter => (data?: IFormatterValueType) => {
    return (data ? (
      <ExternalLink href={data.toString()} />
    ) : undefined) as object;
  };

  /* eslint-disable no-template-curly-in-string */
  const replaceBaseUrl = (r: ClientRepresentation) => {
    if (r.rootUrl) {
      if (!r.rootUrl.startsWith("http") || r.rootUrl.indexOf("$") !== -1) {
        r.rootUrl =
          r.rootUrl
            .replace("${authBaseUrl}", baseUrl)
            .replace("${authAdminUrl}", baseUrl) +
          (r.baseUrl ? r.baseUrl.substr(1) : "");
      }
    }
    return r.rootUrl;
  };

  const data = clients!
    .map((client) => {
      client.baseUrl = replaceBaseUrl(client);
      return client;
    })
    .map((client) => {
      return {
        cells: columns.map((col) =>
          col === "clientId" ? (
            <>
              <Link key={client.id} to={`/clients/${client.id}`}>
                {client.clientId}
                {!client.enabled && <Badge isRead>Disabled</Badge>}
              </Link>
            </>
          ) : (
            client[col]
          )
        ),
        client,
      };
    });
  return (
    <>
      <Table
        variant={TableVariant.compact}
        cells={[
          t("clientID"),
          t("type"),
          { title: t("description"), cellFormatters: [emptyFormatter()] },
          {
            title: t("homeURL"),
            cellFormatters: [externalLink(), emptyFormatter()],
          },
        ]}
        rows={data}
        actions={[
          {
            title: t("common:export"),
            onClick: (_, rowId) => {
              exportClient(data[rowId].client);
            },
          },
          {
            title: t("common:delete"),
            onClick: async (_, rowId) => {
              try {
                await adminClient.clients.del({
                  id: data[rowId].client.id!,
                });
                refresh();
                addAlert(t("clientDeletedSuccess"), AlertVariant.success);
              } catch (error) {
                addAlert(
                  `${t("clientDeleteError")} ${error}`,
                  AlertVariant.danger
                );
              }
            },
          },
        ]}
        aria-label={t("clientList")}
      >
        <TableHeader />
        <TableBody />
      </Table>
    </>
  );
};
