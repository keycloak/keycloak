import React from "react";
import {
  Table,
  TableBody,
  TableHeader,
  TableVariant,
  IFormatter,
  IFormatterValueType,
} from "@patternfly/react-table";
import { Badge } from "@patternfly/react-core";

import { ExternalLink } from "../components/external-link/ExternalLink";
import { ClientRepresentation } from "../model/client-model";

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
  const enabled = (): IFormatter => (data?: IFormatterValueType) => {
    const field = data!.toString();
    const value = field.substring(0, field.indexOf("#"));
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
      return { cells: columns.map((col) => c[col]) };
    });
  return (
    <Table
      variant={TableVariant.compact}
      cells={[
        { title: "Client ID", cellFormatters: [enabled()] },
        "Type",
        { title: "Description", cellFormatters: [emptyFormatter()] },
        {
          title: "Home URL",
          cellFormatters: [externalLink(), emptyFormatter()],
        },
      ]}
      rows={data}
      aria-label="Client list"
    >
      <TableHeader />
      <TableBody />
    </Table>
  );
};
