import React, { useContext } from "react";
import { useTranslation } from "react-i18next";
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
  const [add, Alerts] = useAlerts();

  const convertClientId = (clientId: string) =>
    clientId.substring(0, clientId.indexOf("#"));
  const enabled = (): IFormatter => (data?: IFormatterValueType) => {
    const field = data!.toString();
    const value = convertClientId(field);
    return field.indexOf("true") !== -1 ? (
      <>{value}</>
    ) : (
      <>
        {value} <Badge isRead>Disabled</Badge>
      </>
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
  const replaceBaseUrl = (r: ClientRepresentation) =>
    r.rootUrl &&
    r.rootUrl
      .replace("${authBaseUrl}", baseUrl)
      .replace("${authAdminUrl}", baseUrl) +
      (r.baseUrl ? r.baseUrl.substr(1) : "");

  const data = clients!
    .map((r) => {
      r.clientId = r.clientId + "#" + r.enabled;
      r.baseUrl = replaceBaseUrl(r);
      return r;
    })
    .map((c) => {
      return { cells: columns.map((col) => c[col]), client: c };
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
              clientCopy.clientId = convertClientId(clientCopy.clientId);
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
                  `/admin/realms/master/clients/${data[rowId].client.id}`
                );
                add(t("clientDeletedSucess"), AlertVariant.success);
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
