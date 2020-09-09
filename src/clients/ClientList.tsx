import React, { useContext } from "react";
import {
  Table,
  TableBody,
  TableHeader,
  TableVariant,
  IFormatter,
  IFormatterValueType,
} from "@patternfly/react-table";
import { Badge, AlertVariant } from "@patternfly/react-core";
import { saveAs } from "file-saver";

import { ExternalLink } from "../components/external-link/ExternalLink";
import { ClientRepresentation } from "../model/client-model";
import { HttpClientContext } from "../http-service/HttpClientContext";
import { useAlerts } from "../components/alert/Alerts";
import { AlertPanel } from "../components/alert/AlertPanel";
import { useTranslation } from "react-i18next";

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
  const httpClient = useContext(HttpClientContext)!;
  const { t } = useTranslation();
  const [add, alerts, hide] = useAlerts();

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
      <AlertPanel alerts={alerts} onCloseAlert={hide} />
      <Table
        variant={TableVariant.compact}
        cells={[
          { title: t("Client ID"), cellFormatters: [enabled()] },
          t("Type"),
          { title: t("Description"), cellFormatters: [emptyFormatter()] },
          {
            title: t("Home URL"),
            cellFormatters: [externalLink(), emptyFormatter()],
          },
        ]}
        rows={data}
        actions={[
          {
            title: t("Export"),
            onClick: (_, rowId) => {
              const clientCopy = JSON.parse(JSON.stringify(data[rowId].client));
              clientCopy.clientId = convertClientId(clientCopy.clientId);
              delete clientCopy.id;

              if (clientCopy.protocolMappers) {
                for (let i = 0; i < clientCopy.protocolMappers.length; i++) {
                  delete clientCopy.protocolMappers[i].id;
                }
              }

              saveAs(
                new Blob([JSON.stringify(clientCopy, null, 2)], {
                  type: "application/json",
                }),
                clientCopy.clientId + ".json"
              );
            },
          },
          {
            title: t("Delete"),
            onClick: (_, rowId) => {
              try {
                httpClient.doDelete(
                  `/admin/realms/master/clients/${data[rowId].client.id}`
                );
                add(t("The client has been deleted"), AlertVariant.success);
              } catch (error) {
                add(
                  `${t("Could not delete client:")} ${error}`,
                  AlertVariant.danger
                );
              }
            },
          },
        ]}
        aria-label="Client list"
      >
        <TableHeader />
        <TableBody />
      </Table>
    </>
  );
};
