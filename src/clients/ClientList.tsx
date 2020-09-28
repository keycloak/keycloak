import React, { useContext } from "react";
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
import FileSaver from "file-saver";

import { ExternalLink } from "../components/external-link/ExternalLink";
import { HttpClientContext } from "../http-service/HttpClientContext";
import { useAlerts } from "../components/alert/Alerts";
import { ClientRepresentation } from "./models/client-model";
import { RealmContext } from "../components/realm-context/RealmContext";

type ClientListProps = {
  clients?: ClientRepresentation[];
  baseUrl: string;
};

const columns: (keyof ClientRepresentation)[] = [
  "clientId",
  "protocol",
  "description",
  "baseUrl",
];

export const ClientList = ({ baseUrl, clients }: ClientListProps) => {
  const { t } = useTranslation("clients");
  const httpClient = useContext(HttpClientContext)!;
  const { realm } = useContext(RealmContext);
  const [add, Alerts] = useAlerts();

  const enabled = (): IFormatter => (data?: IFormatterValueType) => {
    const field = data!.toString();
    const [id, clientId, disabled] = field.split("#");
    return (
      <Link to={`client-settings/${id}`}>
        {clientId}
        {disabled !== "true" && <Badge isRead>Disabled</Badge>}
      </Link>
    );
  };

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
      client.clientId = `${client.id}#${client.clientId}#${client.enabled}`;
      client.baseUrl = replaceBaseUrl(client);
      return client;
    })
    .map((column) => {
      return { cells: columns.map((col) => column[col]), client: column };
    });
  return (
    <>
      <Alerts />
      <Table
        variant={TableVariant.compact}
        cells={[
          { title: t("clientID"), cellFormatters: [enabled()] },
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
              const clientCopy = JSON.parse(JSON.stringify(data[rowId].client));
              const [, orgClientId] = clientCopy.clientId.split("#");
              clientCopy.clientId = orgClientId;
              delete clientCopy.id;

              if (clientCopy.protocolMappers) {
                for (let i = 0; i < clientCopy.protocolMappers.length; i++) {
                  delete clientCopy.protocolMappers[i].id;
                }
              }

              FileSaver.saveAs(
                new Blob([JSON.stringify(clientCopy, null, 2)], {
                  type: "application/json",
                }),
                clientCopy.clientId + ".json"
              );
            },
          },
          {
            title: t("common:delete"),
            onClick: (_, rowId) => {
              try {
                httpClient.doDelete(
                  `/admin/realms/${realm}/clients/${data[rowId].client.id}`
                );
                add(t("clientDeletedSuccess"), AlertVariant.success);
              } catch (error) {
                add(`${t("clientDeleteError")} ${error}`, AlertVariant.danger);
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
