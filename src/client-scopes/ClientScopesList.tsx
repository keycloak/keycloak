import React from "react";
import { useTranslation } from "react-i18next";
import {
  Table,
  TableBody,
  TableHeader,
  TableVariant,
} from "@patternfly/react-table";

import { ClientScopeRepresentation } from "./models/client-scope";

type ClientScopeListProps = {
  clientScopes: ClientScopeRepresentation[];
};

export const ClientScopeList = ({ clientScopes }: ClientScopeListProps) => {
  const { t } = useTranslation("client-scopes");

  const columns: (keyof ClientScopeRepresentation)[] = [
    "name",
    "description",
    "protocol",
  ];

  const data = clientScopes.map((c) => {
    return { cells: columns.map((col) => c[col]) };
  });

  return (
    <>
      <Table
        variant={TableVariant.compact}
        cells={[
          { title: t("name") },
          { title: t("description") },
          {
            title: t("protocol"),
          },
        ]}
        rows={data}
        actions={[
          {
            title: t("common:export"),
            onClick: () => {},
          },
          {
            title: t("common:delete"),
            onClick: () => {},
          },
        ]}
        aria-label={t("clientScopeList")}
      >
        <TableHeader />
        <TableBody />
      </Table>
    </>
  );
};
